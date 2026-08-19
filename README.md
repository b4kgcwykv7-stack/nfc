# NFC PIX Display — V5

**Documentação completa:** `MANUAL_INSTALACAO_COMPLETO.md`

**Início rápido:** `INICIO_RAPIDO.md`

# NFC PIX Display — V4 Produção

Sistema SaaS para displays NFC/PIX.

## Recursos
- PostgreSQL em produção (SQLite apenas como fallback local)
- Login administrativo com senha hash
- Sessão protegida por Flask-Login
- Cadastro de clientes
- URLs `/p/<slug>` para gravar na NTAG213
- Múltiplos links por cliente
- QR Code e PIX na página pública
- Suporte WhatsApp: 84 99937-7777
- Registro de acessos e consulta de estatísticas
- HTTPS suportado pela plataforma de hospedagem

## Execução local
1. `pip install -r requirements.txt`
2. Configure `SECRET_KEY`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`.
3. `python init_admin.py`
4. `python app.py`
5. Acesse `/login`.

## Produção
O `render.yaml` cria um Web Service e PostgreSQL. Configure `ADMIN_PASSWORD` e publique.
Depois de entrar, cadastre os clientes. Grave no NTAG213 apenas a URL pública `/p/slug`.

## Segurança
Use HTTPS, senha forte, SECRET_KEY aleatória e banco PostgreSQL. Não coloque credenciais reais no código.
