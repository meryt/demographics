package com.meryt.demographics.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.meryt.demographics.request.AdvanceToDatePost;
import com.meryt.demographics.response.calendar.CalendarDayEvent;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ConflictException;
import com.meryt.demographics.service.CalendarService;
import com.meryt.demographics.service.CheckDateService;

@RestController
public class CalendarController {

    private final CheckDateService checkDateService;
    private final CalendarService calendarService;

    public CalendarController(@Autowired @NonNull CheckDateService checkDateService,
                              @Autowired @NonNull CalendarService calendarService) {
        this.checkDateService = checkDateService;
        this.calendarService = calendarService;
    }

    @RequestMapping(value = "/api/calendar/currentDate", method = RequestMethod.GET)
    public LocalDate getCurrentDate() {
        return checkDateService.getCurrentDate();
    }

    @RequestMapping(value = "/api/calendar/currentDate", method = RequestMethod.POST)
    public Map<LocalDate, List<CalendarDayEvent>> advanceToDate(@RequestBody AdvanceToDatePost nextDatePost) {
        LocalDate currentDate = checkDateService.getCurrentDate();

        nextDatePost.validate();

        if (currentDate == null) {
            throw new ConflictException("Cannot advance to a date: current date is null");
        } else if (nextDatePost.getDate() != null && currentDate.isAfter(nextDatePost.getDate())) {
            throw new BadRequestException(String.format("Current date %s is after requested advance-to date %s",
                    currentDate, nextDatePost.getDate()));
        }

        LocalDate toDate;
        if (nextDatePost.getAdvanceDays() != null) {
            toDate = currentDate.plusDays(nextDatePost.getAdvanceDays());
        } else {
            toDate = nextDatePost.getDate();
        }

        return calendarService.advanceToDay(toDate, nextDatePost);
    }

}
