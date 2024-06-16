package com.wipro.service;

import com.wipro.entity.BookingEntity;

import java.util.List;

public interface BookingService {

    List<BookingEntity> getAllBookingDetail();

    BookingEntity newBooking(BookingEntity bookingEntity);

    BookingEntity getBookingDetails(Integer id);

    BookingEntity updateBooking(BookingEntity bookingEntity);

    void deleteBooking(Integer id);
}
