-- A V1 declarou raw_text_hash como CHAR(64). O Postgres implementa CHAR como
-- bpchar, e o Hibernate (ddl-auto=validate) recusa: CHAR != VARCHAR.
--
-- A correcao vem em uma migration NOVA, e nao editando a V1: a V1 ja foi
-- aplicada em ambiente de desenvolvimento e no CI, e alterar uma migration
-- aplicada muda o checksum e quebra o Flyway em todo ambiente existente.
ALTER TABLE exam_source
    ALTER COLUMN raw_text_hash TYPE VARCHAR(64);
