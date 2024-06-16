package com.wipro.service;

import com.wipro.dto.PaymentDto;
import com.wipro.dto.PropertyDto;
import com.wipro.entity.BookingEntity;
import com.wipro.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

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
        ResponseEntity<PropertyDto> forEntity = restTemplate.getForEntity("http://localhost:9109/property/{id}", PropertyDto.class, bookingEntity.getPropertyId());
        if (forEntity.getStatusCode().is2xxSuccessful()) {
            Integer availableRooms = forEntity.getBody().getAvailableRooms();
            if (availableRooms >= bookingEntity.getNumRooms()) {
                PropertyDto body = forEntity.getBody();
                body.setLockForBook(true);
                HttpEntity<PropertyDto> propertyRequestEntity = new HttpEntity<PropertyDto>(body);
                HttpEntity<PropertyDto> propertyResponse = restTemplate.exchange("http://localhost:9109/property", HttpMethod.PUT, propertyRequestEntity, PropertyDto.class);
                bookingEntity.setBookingStatus("INITIATED");
                savedBooking = bookingRepository.save(bookingEntity);
                ResponseEntity<PaymentDto> paymentDtoResponseEntity = executePayment(savedBooking);
                if(paymentDtoResponseEntity.getStatusCode().is2xxSuccessful()){
                    PropertyDto updateProperty = propertyResponse.getBody();
                    updateProperty.setLockForBook(false);
                    int remainingRooms = availableRooms - bookingEntity.getNumRooms();
                    updateProperty.setAvailableRooms(remainingRooms);
                    HttpEntity<PropertyDto> updatePropertyEntity = new HttpEntity<>(updateProperty);
                    HttpEntity<PropertyDto> response = restTemplate.exchange("http://localhost:9109/property", HttpMethod.PUT, updatePropertyEntity, PropertyDto.class);
                    bookingEntity.setBookingStatus("BOOKED");
                    savedBooking = bookingRepository.save(bookingEntity);
                }
            }
        }
        return savedBooking;

    }
    @CircuitBreaker(name = "paymentCircuitBreaker", fallbackMethod = "showServiceDown")
    public ResponseEntity<PaymentDto> executePayment(BookingEntity savedBooking) {
        PaymentDto paymentDto = PaymentDto.builder().amount("100").bookingId(savedBooking.getId().toString()).build();
        HttpEntity<PaymentDto> paymentEntity = new HttpEntity<>(paymentDto);
        ResponseEntity<PaymentDto> paymentDtoResponseEntity = restTemplate.exchange("http://localhost:9111/pay", HttpMethod.POST, paymentEntity,PaymentDto.class);
        return paymentDtoResponseEntity;
    }

    @Override
    public BookingEntity getBookingDetails(Integer id) {
        return bookingRepository.findById(id).get();
    }

    @Override
    public BookingEntity updateBooking(BookingEntity bookingEntity) {
        return bookingRepository.save(bookingEntity);
    }
}