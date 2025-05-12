package com.fibiyo.ecommerce.application.exception;

import com.fibiyo.ecommerce.application.dto.ApiResponse; // Standart yanıt DTO'muz
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // @PreAuthorize yetersiz rol hatası
import org.springframework.validation.FieldError; // Validation hataları için
import org.springframework.web.bind.MethodArgumentNotValidException; // @Valid DTO hataları için
import org.springframework.web.bind.annotation.ExceptionHandler; // Hataları yakalamak için
import org.springframework.web.bind.annotation.ResponseStatus; // Hata kodunu ayarlamak için
import org.springframework.web.bind.annotation.RestControllerAdvice; // Global handler olduğunu belirtir
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // Controller'larda fırlatılan exception'ları global olarak yakalar
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Kendi Custom Exception'larımızı Yakalama

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Yanıt kodunu ayarla (sınıftaki @ResponseStatus'u ezer veya doğrular)
    public ResponseEntity<ApiResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource Not Found: {}", ex.getMessage());
        ApiResponse apiResponse = new ApiResponse(false, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse> handleBadRequestException(BadRequestException ex, WebRequest request) {
        logger.warn("Bad Request: {}", ex.getMessage());
        ApiResponse apiResponse = new ApiResponse(false, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }



    @ExceptionHandler(ConflictException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ResponseEntity<ApiResponse> handleConflictException(ConflictException ex, WebRequest request) {
    logger.warn("Conflict Error: {}", ex.getMessage());
    ApiResponse apiResponse = new ApiResponse(false, ex.getMessage());
    return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
}

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiResponse> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        logger.warn("Forbidden Access: {}", ex.getMessage());
        ApiResponse apiResponse = new ApiResponse(false, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

     @ExceptionHandler(UnauthorizedException.class)
     @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ApiResponse> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        logger.warn("Unauthorized Access: {}", ex.getMessage());
        ApiResponse apiResponse = new ApiResponse(false, ex.getMessage());
         return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
     }


    // Spring Validation (@Valid) Hatalarını Yakalama
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        logger.warn("Validation Errors: {}", errors);
        // Hataları içeren bir mesaj oluştur veya direkt map'i data olarak gönder
         String combinedErrorMessage = errors.entrySet().stream()
               .map(entry -> entry.getKey() + ": " + entry.getValue())
               .reduce((msg1, msg2) -> msg1 + ", " + msg2)
               .orElse("Validasyon hataları bulundu.");

        // ApiResponse data alanına hataları ekleyebiliriz
        ApiResponse apiResponse = new ApiResponse(false, "Validasyon hatası!", errors);
         // veya: ApiResponse apiResponse = new ApiResponse(false, combinedErrorMessage);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    // Spring Security @PreAuthorize yetersiz rol hatası
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("Access Denied: User does not have the required role(s). {}", ex.getMessage());
        ApiResponse apiResponse = new ApiResponse(false, "Bu işlemi gerçekleştirmek için yetkiniz bulunmuyor.");
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }


    // Diğer beklenmedik tüm hataları yakalama (Internal Server Error)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse> handleAllUncaughtException(Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred: {}", ex.getMessage(), ex); // Tam stack trace'i logla
        ApiResponse apiResponse = new ApiResponse(false, "Beklenmedik bir sunucu hatası oluştu. Lütfen daha sonra tekrar deneyin.");
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

/*
 * @RestControllerAdvice: Bu sınıfın global bir exception handler olduğunu belirtir.
@ExceptionHandler: Hangi exception sınıfının yakalanacağını belirtir.
Metodlar: Yakalanan exception'ı parametre olarak alır. Loglama yapar ve standart ApiResponse formatında ResponseEntity döndürür.
@ResponseStatus: Dönen HTTP durum kodunu ayarlar (opsiyoneldir, ResponseEntity içinde de ayarlanabilir).
MethodArgumentNotValidException: @Valid annotation'ı ile işaretlenmiş DTO'lardaki validasyon hatalarını yakalar ve hangi alanda hangi hata olduğunu içeren bir yanıt döner.
AccessDeniedException: @PreAuthorize gibi yetkilendirme annotation'larından kaynaklanan hataları yakalar.
Exception.class: Diğer tüm yakalanmayan exception'ları yakalayarak genel bir 500 Internal Server Error yanıtı döndürür ve hatayı detaylı loglar. Bu, kullanıcının anlamsız stack trace'ler görmesini engeller.
 */