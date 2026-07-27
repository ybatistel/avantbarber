package com.avantbarber.avant.infra;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RestErrorMessage {

    private LocalDateTime timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;

}