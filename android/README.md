# AIFacial — App de exemplo do SDK Android

App Android de referência para o **SDK AIFacial**: reconhecimento facial **offline**.

O exemplo usa o SDK **exatamente como um app cliente**: a biblioteca é baixada do repositório
Maven da AIFacial com uma linha no Gradle, sem acesso ao código do SDK. Use-o como ponto de
partida para o seu app ou como referência de cada recurso.

---

## O que o exemplo mostra

| Recurso | Onde ver |
|---|---|
| **Preparar**: baixa o modelo de IA (na 1ª vez) e sincroniza as pessoas da empresa | tela inicial |
| **Só baixar a IA**: baixa (ou atualiza) o modelo sem mexer nas pessoas do aparelho | botão "Só baixar a IA" |
| **Sincronizar só algumas pessoas**: informe os IDs e baixe só os templates delas | campo "IDs das pessoas" na tela inicial |
| **Apagar as pessoas do aparelho** (mantém a IA), para baixar de novo | botão "Apagar pessoas do aparelho" |
| **Reconhecimento facial** offline, com a pessoa reconhecida, a foto e a distância | `ReconhecimentoActivity` |
| **Em dúvida entre duas pessoas**: orienta "aproxime-se", "olhe de frente"... (status `AMBIGUOUS`) | `ReconhecimentoActivity` (laranja) |
| **Prova de vida** *(opcional)*: pede ações sorteadas (virar o rosto, piscar, sorrir...) contra fotos e vídeos | marque "Prova de vida" |
| **Auditoria** *(opcional)*: foto de cada reconhecimento enviada ao servidor automaticamente | marque "Auditoria" |
| **Cadastro de faces** pelo celular: login do painel → pessoa **deste aparelho** → captura guiada → envio | `PessoasActivity`, `CadastroFaceActivity` |

---

## Requisitos

- Android Studio (ou JDK 17 + Android SDK)
- Aparelho com **Android 8.0 (API 26)** ou superior e câmera (o emulador não serve para o reconhecimento)
- Acesso ao painel AIFacial do seu cliente:
  - **login** do painel;
  - **chave Maven** (painel → **Credenciais** → **Gerar chave Maven**);
  - **ID da empresa** e **token mobile** (painel → **Credenciais**);
  - **token da empresa** (painel → **Empresas**, ou `companytoken` da API `company_add`/`company_list`).

---

## Configuração

Todas as configurações ficam dentro do projeto.

### 1. Acesso ao repositório Maven — `gradle.properties`

```properties
aifacialUser=LOGIN_DO_PAINEL
aifacialPassword=CHAVE_MAVEN
```

A chave Maven aparece **uma única vez**, quando é gerada. Se não tiver mais, gere uma nova
(a anterior deixa de valer).

Sem a chave, o Gradle para com a mensagem
*"Coloque a chave Maven em aifacialPassword..."*.

### 2. Valores pré-preenchidos no app — `local.properties` *(opcional)*

```properties
sdk.dir=CAMINHO_DO_ANDROID_SDK

aifacial.companyId=ID_DA_EMPRESA
aifacial.mobileToken=TOKEN_MOBILE
aifacial.companyToken=TOKEN_DA_EMPRESA
aifacial.loginUser=LOGIN_DO_PAINEL
aifacial.loginPassword=SENHA_DO_PAINEL
```

Valores em branco (ou começando com `COLE_AQUI`) ficam vazios no app, e dá para digitar na tela.

---

## Compilar e instalar

```bash
./gradlew :app:installDebug
```

No Windows: `gradlew.bat :app:installDebug`. Ou abra a pasta no Android Studio e rode o módulo `app`.

---

## Como usar o app

1. Confira o **ID da empresa**, o **token mobile** e o **token da empresa** na tela inicial.
2. Toque em **Preparar**: baixa a IA (só na primeira vez) e sincroniza as pessoas.
   Ao terminar, aparece *"IA pronta · N pessoas no aparelho"*.
3. *(Opcional)* Para trabalhar só com algumas pessoas: em vez de **Preparar**, toque em
   **Só baixar a IA** (ou em **Apagar pessoas do aparelho**, se já preparou), digite os IDs
   (separados por vírgula) e toque em **Sincronizar só estas pessoas**.
   O app mostra quantas foram baixadas e quais IDs não têm face ou não existem.
4. Toque em **Reconhecimento facial** e permita o uso da câmera.
   - Verde: **Reconhecido**, com o nome, o ID e a distância (menor = mais parecido).
   - Amarelo: parecido com alguém, mas ainda não confirmado.
5. *(Opcional)* Marque **Prova de vida** e/ou **Auditoria** antes de abrir o reconhecimento.
6. **Cadastro de faces**: entre com o login do painel, escolha a pessoa (a lista mostra as pessoas
   sincronizadas neste aparelho, com busca por nome ou ID), posicione o rosto
   (o app orienta e captura sozinho) e toque em **Enviar**.

---

## O código em 3 passos

A dependência (`app/build.gradle`):

```gradle
implementation "br.com.aifacial:facial-sdk:1.5.0"
```



```java
// 1. Configurar
AIFacial.init(context, new AIFacialConfig.Builder(empresa, tokenMobile, tokenEmpresa)
        .liveness(LivenessMode.ACTIVE)   // opcional
        .audit(true)                     // opcional
        .build());

// 2. Preparar
AIFacial.prepare(new PrepareCallback() {
    @Override public void onReady(int personCount) { /* pronto */ }
    @Override public void onError(AIFacialException e) { /* e.getCode() */ }
});

// 3. Reconhecer
AIFacial.startRecognition(this, previewView, result -> {
    if (result.isNewEvent()) {
        // primeira vez que esta pessoa é reconhecida diante da câmera:
        // result.getPersonId(), result.getTimestamp(), result.getEventId()
        // → aqui o seu app registra o ponto
    }
});
```

---

## Estrutura

```
ExemploSDK/
├── app/src/main/java/br/com/aifacial/exemplo/
│   ├── MainActivity.java            configuração, preparar, opções, login do painel
│   ├── ReconhecimentoActivity.java  câmera de reconhecimento (status, prova de vida)
│   ├── PessoasActivity.java         pessoas deste aparelho, com foto e busca
│   ├── CadastroFaceActivity.java    captura guiada e envio da face
│   └── Sessao.java                  login do painel enquanto o app está aberto
├── gradle/libs.versions.toml        versões (inclusive a do SDK: aifacialSdk)
├── gradle.properties                acesso ao Maven
└── local.properties                 Android SDK e valores pré-preenchidos
```

Para usar outra versão do SDK, altere `aifacialSdk` em `gradle/libs.versions.toml`.

---

## Problemas comuns

| Sintoma | Solução |
|---|---|
| *"Coloque a chave Maven em aifacialPassword..."* | preencha a chave Maven no `gradle.properties` |
| `Could not resolve br.com.aifacial:facial-sdk` / 401 | login ou chave Maven errados, ou cliente inativo: gere uma nova chave |
| *"Empresa ou token mobile inválido"* / *"Token is invalid"* | confira o ID da empresa e o token mobile (Credenciais) e o token **dessa** empresa (Empresas) |
| *"Toque em Preparar primeiro"* | a IA ainda não foi baixada neste aparelho |
| *"Nenhuma pessoa sincronizada"* | não há pessoas com face cadastrada na empresa |
| *"Usuário ou senha inválidos"* / *"Sessão expirada"* | refaça o login do painel no Cadastro de faces |

---

## Segurança

- Depois de preencher o `gradle.properties` e o `local.properties` com credenciais reais
  (chave Maven, token, senha do painel), **não suba esses arquivos para um repositório público**.
- Fotos e templates faciais são **dados biométricos** (LGPD): informe as pessoas e obtenha o
  consentimento antes de usar o reconhecimento e a auditoria.
- Num app de produção, controle quem pode acessar o **Cadastro de faces**.

---

© AIFacial · [aifacial.com.br](https://aifacial.com.br)
