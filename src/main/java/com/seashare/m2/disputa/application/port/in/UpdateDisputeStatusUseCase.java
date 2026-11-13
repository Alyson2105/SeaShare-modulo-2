package com.seashare.m2.disputa.application.port.in;

import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusCommand;
import com.seashare.m2.disputa.application.dto.UpdateDisputeStatusResult;

public interface UpdateDisputeStatusUseCase { // unica operacion de transicion de la disputa (CU-17)

    UpdateDisputeStatusResult update(UpdateDisputeStatusCommand command); // valida, transiciona y audita
}
