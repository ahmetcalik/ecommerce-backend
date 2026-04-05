package com.project.ecommerce_backend.business.dtos.responses.return_request;

import com.project.ecommerce_backend.entities.enums.ReturnReasonEnum;
import com.project.ecommerce_backend.entities.enums.ReturnStatusEnum;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ReturnRequestResponse {
    private Long id;
    private Long orderId;
    private String productName;
    private ReturnReasonEnum reason;
    private String customReason;
    private ReturnStatusEnum status;
    private OffsetDateTime cDate;
}