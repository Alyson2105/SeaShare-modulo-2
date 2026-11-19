package com.seashare.m2.disputa.application.port.in;

import com.seashare.m2.disputa.application.dto.DisputeListView;

import java.util.List;
import java.util.UUID;

public interface ListDisputesUseCase {

    List<DisputeListView> list(UUID userId, String role, UUID reservationId);
}
