-- Remove colunas legadas da tabela contatos que nao existem mais na entidade Contato.java.
-- 'local' foi substituida por 'localidade'; 'is_ramal'/'is_whatsapp' por 'tipo_contato'/'meio_de_contato'.
-- ATENCAO: se houver dados em 'local' relevantes em algum ambiente, migrar antes de aplicar
-- (ex.: UPDATE contatos SET localidade = local WHERE localidade IS NULL OR localidade = '';).
-- Somente aplica se a tabela contatos ja existir (bases legadas). Em banco novo,
-- o schema e criado pelo Hibernate (ddl-auto=update).
DO
$$
BEGIN
    IF
EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'contatos') THEN
ALTER TABLE contatos DROP COLUMN IF EXISTS local;
ALTER TABLE contatos DROP COLUMN IF EXISTS is_ramal;
ALTER TABLE contatos DROP COLUMN IF EXISTS is_whatsapp;

-- Alinha NOT NULL com a entidade: apenas 'unidade' e 'comarca' sao obrigatorios em Contato.java.
ALTER TABLE contatos
    ALTER COLUMN setor DROP NOT NULL;
ALTER TABLE contatos
    ALTER COLUMN telefone DROP NOT NULL;
ALTER TABLE contatos
    ALTER COLUMN endereco DROP NOT NULL;
ALTER TABLE contatos
    ALTER COLUMN localidade DROP NOT NULL;
END IF;
END $$;
