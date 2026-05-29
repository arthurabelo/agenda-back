-- Remove colunas legadas da tabela contatos que nao existem mais na entidade Contato.java.
-- 'local' foi substituida por 'localidade'; 'is_ramal'/'is_whatsapp' por 'tipo_contato'/'meio_de_contato'.
-- ATENCAO: se houver dados em 'local' relevantes em algum ambiente, migrar antes de aplicar
-- (ex.: UPDATE contatos SET localidade = local WHERE localidade IS NULL OR localidade = '';).
ALTER TABLE contatos DROP COLUMN IF EXISTS local;
ALTER TABLE contatos DROP COLUMN IF EXISTS is_ramal;
ALTER TABLE contatos DROP COLUMN IF EXISTS is_whatsapp;

-- Alinha NOT NULL com a entidade: apenas 'unidade' e 'comarca' sao obrigatorios em Contato.java.
ALTER TABLE contatos ALTER COLUMN setor DROP NOT NULL;
ALTER TABLE contatos ALTER COLUMN telefone DROP NOT NULL;
ALTER TABLE contatos ALTER COLUMN endereco DROP NOT NULL;
ALTER TABLE contatos ALTER COLUMN localidade DROP NOT NULL;
