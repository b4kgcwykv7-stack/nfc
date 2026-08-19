# MANUAL COMPLETO — NFC PIX DISPLAY SaaS V5

## 1. Visão geral

O NFC PIX Display é um sistema para criar páginas personalizadas para displays de mesa com NFC NTAG213 e QR Code.

Fluxo:

NTAG213 → URL exclusiva → página do cliente → PIX + QR Code + WhatsApp + Instagram + links.

Exemplo:

https://SEU-DOMINIO.com/p/tiago

A etiqueta NFC deve armazenar somente a URL. Assim, você pode alterar os dados do cliente sem precisar regravar a etiqueta.

---

# 2. O que você precisa

Para colocar o sistema online:

- Computador com Windows/macOS/Linux
- Conta em um serviço de hospedagem compatível com Python
- Banco PostgreSQL
- Um domínio próprio (opcional, mas recomendado)
- Celular com NFC
- Etiquetas NTAG213
- NFC Tools ou aplicativo equivalente
- Chave PIX do cliente

Para uso local:

- Python 3.12+
- Git é recomendado
- Navegador atualizado

---

# 3. Estrutura do projeto

Arquivos principais:

- `app.py` — aplicação Flask
- `requirements.txt` — dependências
- `init_admin.py` — criação do administrador
- `templates/login.html` — login
- `templates/index.html` — painel administrativo
- `templates/client.html` — página pública do cliente
- `render.yaml` — configuração de implantação
- `.env.example` — exemplo das variáveis
- `Dockerfile` — imagem para implantação
- `README.md` — documentação resumida

---

# 4. Instalação no Windows

## 4.1 Instalar Python

Baixe o Python no site oficial:

https://www.python.org/downloads/

Durante a instalação, marque:

[ ] Add Python to PATH

Depois abra o PowerShell e teste:

```powershell
python --version
```

Se aparecer algo como:

Python 3.12.x

a instalação está correta.

---

# 5. Criar ambiente virtual

Entre na pasta do projeto:

```powershell
cd C:\caminho\nfc_pix_saas_v5
```

Crie o ambiente:

```powershell
python -m venv .venv
```

Ative:

```powershell
.venv\Scripts\Activate.ps1
```

Se o PowerShell bloquear a execução:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

Depois ative novamente.

---

# 6. Instalar dependências

Com o ambiente virtual ativado:

```powershell
pip install -r requirements.txt
```

---

# 7. Configuração local

Para testes locais, você pode utilizar SQLite.

Defina:

```text
DATABASE_URL=sqlite:///nfc_pix.db
```

Defina também uma chave secreta:

```text
SECRET_KEY=uma-chave-longa-e-aleatoria
```

E os dados do administrador:

```text
ADMIN_EMAIL=admin@seudominio.com
ADMIN_PASSWORD=troque-esta-senha
```

O suporte está configurado para:

```text
WhatsApp: (84) 99937-7777
```

Link:

https://wa.me/5584999377777

---

# 8. Criar administrador

Com as variáveis configuradas:

```powershell
python init_admin.py
```

O sistema criará o usuário administrador.

IMPORTANTE:

Use uma senha forte e não compartilhe o arquivo de configuração.

---

# 9. Executar localmente

Execute:

```powershell
python app.py
```

Abra:

http://localhost:5000/login

Entre com o e-mail e a senha cadastrados.

---

# 10. Banco PostgreSQL para produção

Para produção, prefira PostgreSQL.

A variável principal será:

```text
DATABASE_URL=postgresql://USUARIO:SENHA@HOST:5432/BANCO
```

Não coloque essa informação publicamente em arquivos enviados para outras pessoas.

A hospedagem deve fornecer a URL de conexão.

---

# 11. Publicação usando Render

O projeto possui `render.yaml`.

Passos gerais:

1. Crie uma conta no Render.
2. Crie um repositório no GitHub.
3. Envie o projeto para o GitHub.
4. No Render, conecte o repositório.
5. Crie o serviço web.
6. Crie o banco PostgreSQL.
7. Configure as variáveis de ambiente.
8. Faça o deploy.

Variáveis recomendadas:

```text
DATABASE_URL=<fornecida pelo PostgreSQL>
SECRET_KEY=<valor aleatório forte>
ADMIN_EMAIL=<seu e-mail>
ADMIN_PASSWORD=<sua senha forte>
SUPPORT_WHATSAPP=5584999377777
```

IMPORTANTE:

Nunca publique `ADMIN_PASSWORD` ou credenciais do banco no GitHub.

---

# 12. Primeiro acesso

Depois da publicação:

1. Abra `/login`.
2. Entre com o administrador.
3. Cadastre um cliente.
4. Defina um slug.

Exemplo:

Nome:

```text
Tiago Souza
```

Slug:

```text
tiago
```

A página pública será:

```text
https://SEU-DOMINIO.com/p/tiago
```

---

# 13. Cadastrar um cliente

Preencha:

- Nome
- Slug
- Chave PIX
- Nome do recebedor
- Cidade
- WhatsApp
- Instagram
- Valor, se desejar

Clique em:

**Cadastrar cliente**

---

# 14. Adicionar links

Cada cliente pode possuir vários links.

Exemplos:

```text
Instagram
https://instagram.com/usuario
```

```text
WhatsApp
https://wa.me/5584999999999
```

```text
Site
https://exemplo.com
```

```text
Cardápio
https://exemplo.com/cardapio
```

```text
Localização
https://maps.google.com/...
```

Use sempre URLs completas começando com:

```text
https://
```

---

# 15. Gravar a NTAG213

A NTAG213 possui memória suficiente para uma URL curta, sendo excelente para este projeto.

RECOMENDAÇÃO:

Não grave a chave PIX diretamente na etiqueta.

Grave:

```text
https://SEU-DOMINIO.com/p/tiago
```

Assim, os dados podem ser atualizados posteriormente sem regravar o NFC.

## NFC Tools

No celular:

1. Ative o NFC.
2. Abra NFC Tools.
3. Selecione `Write`.
4. Selecione `Add a record`.
5. Escolha `URL / URI`.
6. Cole a URL do cliente.
7. Toque em `Write`.
8. Encoste o celular na NTAG213.
9. Aguarde a confirmação.
10. Teste novamente.

---

# 16. Teste do display

Teste três situações:

### Teste 1 — NFC

Aproxime um celular compatível.

Deve abrir:

```text
https://SEU-DOMINIO.com/p/cliente
```

### Teste 2 — QR Code

Leia o QR Code.

A mesma página deve abrir.

### Teste 3 — Links

Teste:

- Copiar PIX
- WhatsApp
- Instagram
- Links adicionais

---

# 17. Segurança

Antes de colocar o sistema em produção:

- Use HTTPS.
- Use senha administrativa forte.
- Use SECRET_KEY aleatória.
- Não publique senhas no GitHub.
- Não compartilhe DATABASE_URL.
- Faça backup periódico.
- Utilize PostgreSQL em produção.
- Mantenha as dependências atualizadas.

---

# 18. Domínio próprio

Depois de ter o sistema funcionando:

1. Compre um domínio.
2. Adicione o domínio à hospedagem.
3. Configure o DNS conforme a hospedagem.
4. Aguarde a propagação.
5. Confirme que HTTPS está ativo.

Exemplo:

```text
www.nfcpix.com.br
```

A URL da etiqueta poderá ser:

```text
https://www.nfcpix.com.br/p/cliente
```

---

# 19. Modelo comercial

Uma possibilidade de produto:

### Display NFC + PIX

Inclui:

- Display acrílico
- QR Code
- Etiqueta NTAG213
- Página personalizada
- PIX
- WhatsApp
- Instagram
- Links

Você pode criar uma página exclusiva para cada cliente.

---

# 20. Alteração dos dados

Uma das principais vantagens do sistema:

Se o cliente trocar:

- chave PIX
- WhatsApp
- Instagram
- site
- cardápio

você altera os dados no painel.

A NTAG213 permanece igual.

---

# 21. Estatísticas

A V5 registra acessos às páginas públicas.

Isso permite futuramente apresentar:

- Total de acessos
- Acessos por cliente
- Acessos por período
- Origem NFC/QR
- Horários de maior utilização

A estrutura já possui tabela de acessos no banco.

---

# 22. Backup

Faça backup do PostgreSQL regularmente.

Nunca dependa de uma única cópia do banco.

Recomendação:

- Backup diário
- Backup semanal externo
- Backup antes de atualizações importantes

---

# 23. Problemas comuns

## NFC não abre

Verifique:

- NFC ativado
- etiqueta não danificada
- URL correta
- etiqueta não bloqueada
- celular compatível
- distância entre celular e etiqueta

## NTAG213 não grava

Verifique se a etiqueta está:

- vazia
- desbloqueada
- compatível com NDEF

Faça um teste de leitura no NFC Tools.

## URL abre, mas cliente não existe

Verifique o slug.

Exemplo:

```text
/p/tiago
```

deve corresponder ao cliente com slug:

```text
tiago
```

## Página abre mas PIX está errado

Não regrave automaticamente a etiqueta.

Primeiro corrija os dados do cliente no painel e teste novamente.

## Erro no banco

Confira:

```text
DATABASE_URL
```

e se o PostgreSQL está disponível.

---

# 24. Checklist de publicação

- [ ] Python instalado
- [ ] Dependências instaladas
- [ ] PostgreSQL criado
- [ ] DATABASE_URL configurada
- [ ] SECRET_KEY configurada
- [ ] Administrador criado
- [ ] Login funcionando
- [ ] Cliente cadastrado
- [ ] Página pública funcionando
- [ ] WhatsApp funcionando
- [ ] QR Code funcionando
- [ ] HTTPS funcionando
- [ ] NTAG213 gravada
- [ ] NFC testado
- [ ] Backup configurado

---

# 25. Suporte

O sistema está configurado com o suporte:

**WhatsApp: (84) 99937-7777**

Link:

https://wa.me/5584999377777

---

# 26. Arquitetura recomendada

```text
                 ┌───────────────┐
                 │    NTAG213    │
                 └───────┬───────┘
                         │
                         ▼
                URL /p/cliente
                         │
                         ▼
              ┌──────────────────┐
              │ Página do cliente│
              └────────┬─────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
         PIX        WhatsApp      Links
          │
          ▼
       QR Code

                         │
                         ▼
                  Banco PostgreSQL
                         │
                         ▼
                    Estatísticas
```

---

# 27. Importante sobre a NTAG213

A etiqueta é apenas o identificador físico.

O conteúdo principal fica no servidor.

Isso permite vender o display como um produto atualizável:

**Etiqueta NFC física + serviço digital.**

Se o cliente trocar os dados, você altera o cadastro e mantém a mesma etiqueta.

---

# 28. Próximas evoluções

A arquitetura permite adicionar:

- Área individual do cliente
- Planos mensal/anual
- Assinaturas
- Pagamento recorrente
- QR Code dinâmico
- Editor visual
- Upload de logotipo
- Temas de página
- Domínio personalizado
- Relatórios avançados
- Exportação de estatísticas
- Controle de múltiplos usuários
- API
- Impressão automática da arte do display
- Controle de estoque das etiquetas NTAG213
- Registro do número físico de cada etiqueta
- Status: disponível / gravada / vendida / bloqueada

---

## FIM DO MANUAL

A versão V5 foi preparada para ser a base de um produto comercial de displays NFC/PIX.
