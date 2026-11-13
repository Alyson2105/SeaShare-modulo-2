package com.seashare.m2.disputa.domain.model;

import com.seashare.m2.disputa.domain.exception.DisputeRuleException;
import com.seashare.m2.shared.domain.ErrorCode;

public enum DisputeCategory { // clasificacion preliminar del dano reportado (CU-16 plan seccion 4.1)

    STRUCTURE("structure"),

    ENGINE("engine"),

    EQUIPMENT("equipment"),

    CLEANING("cleaning"),

    DELAY("delay"),

    OTHER("other");

    private final String wireValue; // valor que viaja en la API y se guarda en la base

    DisputeCategory(String wireValue) { // constructor del catalogo
        this.wireValue = wireValue;
    }

    public String wireValue() { // valor snake_case del contrato
        return wireValue;
    }

    public static DisputeCategory fromWire(String value) { // traduce el texto recibido al catalogo
        if (value == null || value.isBlank()) { // categoria opcional
            return null;
        }
        for (DisputeCategory category : values()) { // busca coincidencia exacta
            if (category.wireValue.equalsIgnoreCase(value.trim())) {
                return category;
            }
        }
        throw new DisputeRuleException(ErrorCode.INVALID_PARAMETERS,
                "Unknown damage_category; expected structure, engine, equipment, cleaning, delay or other");
    }

    public static DisputeCategory fromStored(String value) { // traduce el valor persistido sin validar el contrato
        return value == null ? null : fromWire(value);
    }
}
