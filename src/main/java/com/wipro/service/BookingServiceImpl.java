package com.wipro.service;

import com.wipro.dto.ErrorResponse;
import com.wipro.dto.PaymentDto;
import com.wipro.dto.PropertyDto;
import com.wipro.entity.BookingEntity;
import com.wipro.exception.BookingFailedException;
import com.wipro.repository.BookingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    BookingRepository bookingRepository;

    @Override
    public List<BookingEntity> getAllBookingDetail() {
        return bookingRepository.findAll();
    }

    @Override
    public BookingEntity newBooking(BookingEntity bookingEntity) {

        BookingEntity savedBooking = null;
        PropertyDto.builder().availableRooms(bookingEntity.getNumRooms()).build();
        ResponseEntity<PropertyDto> getPropertyResponse = restTemplate.getForEntity("http://localhost:9109/property/{id}", PropertyDto.class, bookingEntity.getPropertyId());
        if (getPropertyResponse.getStatusCode().is2xxSuccessful()) {
            Integer availableRooms = Objects.requireNonNull(getPropertyResponse.getBody()).getAvailableRooms();
            HttpEntity<PropertyDto> propertyRequestEntity = validateIfRoomsAvailable(bookingEntity, availableRooms, getPropertyResponse);
            HttpEntity<PropertyDto> propertyResponse = restTemplate.exchange("http://localhost:9109/property", HttpMethod.PUT, propertyRequestEntity, PropertyDto.class);
            bookingEntity.setBookingStatus("INITIATED");
            savedBooking = bookingRepository.save(bookingEntity);
            ResponseEntity<PaymentDto> paymentDtoResponseEntity = executePayment(savedBooking);
            if (paymentDtoResponseEntity.getStatusCode().is2xxSuccessful()) {
                PropertyDto updateProperty = propertyResponse.getBody();
                updateProperty.setLockForBook(false);
                int remainingRooms = availableRooms - bookingEntity.getNumRooms();
                updateProperty.setAvailableRooms(remainingRooms);
                HttpEntity<PropertyDto> updatePropertyEntity = new HttpEntity<>(updateProperty);
                HttpEntity<PropertyDto> response = restTemplate.exchange("http://localhost:9109/property", HttpMethod.PUT, updatePropertyEntity, PropertyDto.class);
                bookingEntity.setBookingStatus("BOOKED");
                savedBooking = bookingRepository.save(bookingEntity);
            } else {
                bookingEntity.setBookingStatus("FAILED");
                bookingEntity.setBookingDescription("booking failed due to payment failure..! please retry.");
                savedBooking = bookingRepository.save(bookingEntity);
            }
        } else {
            bookingEntity.setBookingStatus("FAILED");
            bookingEntity.setBookingDescription("booking failed due to unavailability of the property service..! please retry.");
            savedBooking = bookingRepository.save(bookingEntity);
        }
        return savedBooking;
    }

    @CircuitBreaker(name = "paymentCircuitBreaker", fallbackMethod = "paymentServiceDown")
    public ResponseEntity<PaymentDto> executePayment(BookingEntity savedBooking) {
        PaymentDto paymentDto = PaymentDto.builder().amount("100").bookingId(savedBooking.getId().toString()).build();
        HttpEntity<PaymentDto> paymentEntity = new HttpEntity<>(paymentDto);
        ResponseEntity<PaymentDto> paymentDtoResponseEntity = restTemplate.exchange("http://localhost:9111/pay", HttpMethod.POST, paymentEntity, PaymentDto.class);
        return paymentDtoResponseEntity;
    }

    public ResponseEntity<PaymentDto> paymentServiceDown() {
        PaymentDto paymentDto = new PaymentDto();
        return new ResponseEntity<>(paymentDto, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public BookingEntity getBookingDetails(Integer id) {
        return bookingRepository.findById(id).get();
    }

    @Override
    public BookingEntity updateBooking(BookingEntity bookingEntity) {
        return bookingRepository.save(bookingEntity);
    }

    @Override
    public void deleteBooking(Integer id) {
        BookingEntity bookingDetails = getBookingDetails(id);
        bookingRepository.delete(bookingDetails);
    }

    private HttpEntity<PropertyDto> validateIfRoomsAvailable(BookingEntity bookingEntity, Integer availableRooms, ResponseEntity<PropertyDto> forEntity) {
        if (availableRooms < bookingEntity.getNumRooms()) {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setErrorMessage("Booking cant be done, due to unavailability of the rooms");
            throw new BookingFailedException(HttpStatus.NOT_FOUND, errorResponse);
        }
        PropertyDto body = forEntity.getBody();
        body.setLockForBook(true);
        HttpEntity<PropertyDto> propertyRequestEntity = new HttpEntity<PropertyDto>(body);
        return propertyRequestEntity;
    }
}
