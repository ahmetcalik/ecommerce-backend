package com.project.ecommerce_backend.business.dtos.responses.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("squid:S1948")
public class SliceResponseDTO<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<T> content;
    private boolean hasNext;
    private int number;
    private int size;
    private int numberOfElements;
    private boolean first;
    private boolean last;
}