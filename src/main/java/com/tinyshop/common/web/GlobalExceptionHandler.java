package com.tinyshop.common.web;

import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException exception, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("message", "요청한 정보를 찾을 수 없습니다.");
        return "error/general";
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView business(BusinessException exception) {
        ModelAndView view = new ModelAndView("error/general");
        view.setStatus(exception.getStatus());
        view.addObject("status", exception.getStatus().value());
        view.addObject("message", exception.getMessage());
        return view;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(Model model) {
        model.addAttribute("status", 409);
        model.addAttribute("message", "다른 요청이 먼저 변경했습니다. 새로고침 후 다시 시도해 주세요.");
        return "error/general";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception exception, HttpServletRequest request, Model model) {
        log.error("Unexpected request failure method={} path={}",
            sanitize(request.getMethod()), sanitize(request.getRequestURI()), exception);
        model.addAttribute("status", 500);
        model.addAttribute("message", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        return "error/general";
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
