package com.seashare.m2.disputa.application.port.in;

import com.seashare.m2.disputa.application.dto.DisputeClaimView;
import com.seashare.m2.disputa.application.dto.RegisterDisputeClaimCommand;

public interface RegisterDisputeClaimUseCase { // puerto de entrada del reclamo del propietario (CU-16)

    DisputeClaimView register(RegisterDisputeClaimCommand command); // registra el reclamo sin cambiar el estado
}
