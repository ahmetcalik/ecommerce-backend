package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.entities.concretes.PaymentType;
import com.project.ecommerce_backend.entities.enums.PaymentTypeEnum;

public interface PaymentTypeService {

    PaymentType getByName(PaymentTypeEnum name);

}