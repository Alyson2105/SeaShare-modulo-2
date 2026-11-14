package com.seashare.m2.disputa.application.port.in;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;

public interface EditDisputeClaimUseCase {
    DisputeClaimView edit(RegisterDisputeClaimCommand command);
}
