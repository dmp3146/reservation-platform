-- Manual additive migration for booking against disponibilidad_servicio.
-- Preserve legacy reserva rows that reference horario_disponible.
BEGIN;

ALTER TABLE reserva
    ALTER COLUMN horario_id DROP NOT NULL;

ALTER TABLE reserva
    ADD COLUMN disponibilidad_servicio_id INTEGER
        REFERENCES disponibilidad_servicio(id),
    ADD COLUMN hora_fin TIME;

ALTER TABLE reserva
    ADD CONSTRAINT ck_reserva_modelo_disponibilidad CHECK (
        (horario_id IS NOT NULL
            AND disponibilidad_servicio_id IS NULL
            AND hora_fin IS NULL)
        OR
        (horario_id IS NULL
            AND disponibilidad_servicio_id IS NOT NULL
            AND hora_fin IS NOT NULL)
    ),
    ADD CONSTRAINT ck_reserva_hora_fin CHECK (
        hora_fin IS NULL OR hora_fin > hora
    );

CREATE UNIQUE INDEX ux_reserva_disponibilidad_fecha_hora_activa
    ON reserva (disponibilidad_servicio_id, fecha, hora)
    WHERE estado = 'activa' AND disponibilidad_servicio_id IS NOT NULL;

COMMIT;
