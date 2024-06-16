package com.wipro.controller;

import com.wipro.entity.BookingEntity;
import com.wipro.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/book")

public class BookingController {
    Logger logger = LoggerFactory.getLogger(BookingController.class);
    @Autowired
    private BookingService bookingService;


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BookingEntity>> getAllBookingDetail() {
        return new ResponseEntity<>(bookingService.getAllBookingDetail(), HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BookingEntity> newBooking(@RequestBody BookingEntity bookingEntity) {
        return new ResponseEntity<BookingEntity>(bookingService.newBooking(bookingEntity), HttpStatus.CREATED);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BookingEntity> getBookingDetails(@PathVariable Integer id) {
        return new ResponseEntity<BookingEntity>(bookingService.getBookingDetails(id), HttpStatus.OK);
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BookingEntity> updateBooking(@RequestBody BookingEntity bookingEntity) {
        return new ResponseEntity<>(bookingService.updateBooking(bookingEntity), HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Integer id) {
        bookingService.deleteBooking(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
