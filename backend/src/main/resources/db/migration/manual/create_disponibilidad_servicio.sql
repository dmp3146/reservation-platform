BEGIN;

CREATE TABLE public.disponibilidad_servicio (
    id           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    servicio_id  INTEGER NOT NULL REFERENCES public.servicio(id),
    fecha        DATE NOT NULL,
    hora_inicio  TIME NOT NULL,
    hora_fin     TIME NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_disponibilidad_servicio_rango
        CHECK (hora_fin > hora_inicio)
);

CREATE INDEX ix_disponibilidad_servicio_fecha
    ON public.disponibilidad_servicio (servicio_id, fecha, hora_inicio);

ALTER TABLE public.disponibilidad_servicio ENABLE ROW LEVEL SECURITY;

CREATE FUNCTION public.check_disponibilidad_servicio_overlap()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $function$
BEGIN
    PERFORM 1
    FROM public.servicio
    WHERE id = NEW.servicio_id
    FOR UPDATE;

    IF EXISTS (
        SELECT 1
        FROM public.disponibilidad_servicio existing
        WHERE existing.servicio_id = NEW.servicio_id
          AND existing.fecha = NEW.fecha
          AND existing.id IS DISTINCT FROM NEW.id
          AND existing.hora_inicio < NEW.hora_fin
          AND existing.hora_fin > NEW.hora_inicio
    ) THEN
        RAISE EXCEPTION 'Availability block overlaps an existing block.'
            USING ERRCODE = '23P01',
                  CONSTRAINT = 'ex_disponibilidad_servicio_no_overlap';
    END IF;
    RETURN NEW;
END;
$function$;

REVOKE ALL ON FUNCTION public.check_disponibilidad_servicio_overlap()
    FROM PUBLIC;

CREATE TRIGGER trg_disponibilidad_servicio_no_overlap
    BEFORE INSERT ON public.disponibilidad_servicio
    FOR EACH ROW
    EXECUTE FUNCTION public.check_disponibilidad_servicio_overlap();

COMMIT;
