package com.stockai.service.interf;

import java.time.LocalDate;

public interface RetrainingService {

    void checkAndTriggerModelRetraining(LocalDate predictionDate);

}

