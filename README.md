# AIFacial SDK

**Reconhecimento facial offline para Android e iOS, pensado para controle de ponto.**

O reconhecimento acontece **no próprio aparelho**, sem internet. O servidor AIFacial cuida do
cadastro das pessoas, da distribuição do modelo de IA, do sincronismo entre aparelhos e da auditoria.

O SDK reconhece as pessoas; o que fazer com o reconhecimento (registrar o ponto, liberar uma
catraca etc.) é decisão do seu app.

Este repositório traz **apps de exemplo** prontos que usam o SDK exatamente como o seu app vai usar:

| Plataforma | Exemplo | SDK |
|---|---|---|
| Android | [`android/`](android) | `br.com.aifacial:facial-sdk:1.5.0` (Maven) |
| iOS | [`ios/`](ios) | `AIFacialSDK 1.5.0` (CocoaPods) |

---

## Recursos

- **Reconhecimento offline** com detecção de rosto e template facial.
- **Sincronismo incremental**: cada aparelho baixa só as pessoas novas ou alteradas.
- **Sincronismo por lista**: o aparelho baixa só as pessoas que você informar (por ID).
- **Em dúvida, não arrisca**: quando duas pessoas cadastradas ficam parecidas demais, o SDK pede uma
  leitura melhor ("aproxime-se", "olhe de frente"...) em vez de reconhecer a pessoa errada.
- **Prova de vida** *(opcional)*: pede ações sorteadas (virar o rosto, olhar para cima ou para baixo,
  piscar, sorrir, aproximar ou afastar), cada uma com tempo curto, impedindo o uso de fotos e vídeos gravados.
- **Auditoria** *(opcional)*: a foto de cada reconhecimento é enviada ao servidor automaticamente,
  mesmo que não haja internet na hora.
- **Cadastro de faces pelo celular**: orienta a pessoa, captura sozinho quando a foto está boa e
  envia ao servidor.
- **Evento único por pessoa**: um indicador diz quando é a primeira vez que a pessoa foi reconhecida
  diante da câmera, para o seu app registrar o ponto uma vez só.

---

## Antes de começar

Você precisa de um cliente ativo no painel AIFacial e, no painel, em **Credenciais**:

| Item | Para quê |
|---|---|
| **Login do painel** | usuário do repositório do SDK e login do cadastro de faces |
| **Chave Maven** ("Gerar chave Maven") | senha do repositório do SDK (Android e iOS). Aparece **uma única vez** ao ser gerada |
| **ID da empresa** | identifica a empresa das pessoas que o aparelho vai reconhecer |
| **Token mobile** | autoriza o aparelho a baixar o modelo e as pessoas |

E, em **Empresas**, o **token da empresa** (`companytoken` da API `company_add`/`company_list`),
obrigatório a partir da 1.5.0.

As pessoas são cadastradas pela **API de integração** da AIFacial (ou pelo cadastro de faces do app).

---

## Android

**Requisitos:** Android 8.0 (API 26) ou superior, câmera.

### Instalação

Credenciais em `~/.gradle/gradle.properties` (fora do projeto):

```properties
aifacialUser=LOGIN_DO_PAINEL
aifacialPassword=CHAVE_MAVEN
```

`settings.gradle`:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url "https://maven.aifacial.com.br"
            credentials {
                username = providers.gradleProperty("aifacialUser").get()
                password = providers.gradleProperty("aifacialPassword").get()
            }
        }
    }
}
```

`app/build.gradle`:

```gradle
implementation "br.com.aifacial:facial-sdk:1.5.0"
```

As permissões de câmera e internet já estão no SDK.

### Uso

```java
// 1. Configurar (uma vez, por exemplo no Application)
AIFacial.init(context, new AIFacialConfig.Builder("ID_DA_EMPRESA", "TOKEN_MOBILE", "TOKEN_DA_EMPRESA")
        .liveness(LivenessMode.ACTIVE)   // opcional: prova de vida
        .audit(true)                     // opcional: auditoria
        .build());

// 2. Preparar: baixa a IA (1ª vez) e sincroniza as pessoas
AIFacial.prepare(new PrepareCallback() {
    @Override public void onReady(int personCount) { /* pronto */ }
    @Override public void onError(AIFacialException e) { /* e.getCode() */ }
});

// 3. Reconhecer (peça a permissão de câmera antes)
AIFacial.startRecognition(this, previewView, result -> {
    if (result.isNewEvent()) {
        // result.getPersonId(), result.getPersonName(), result.getTimestamp(), result.getEventId()
        // → aqui o seu app registra o ponto
    }
});
```

Exemplo completo: [`android/`](android) — veja o [README do exemplo](android/README.md).

---

## iOS

**Requisitos:** iOS 16 ou superior, iPhone com câmera, Xcode e CocoaPods.

### Instalação

Credenciais do repositório no `~/.netrc` (fora do projeto):

```
machine maven.aifacial.com.br login LOGIN_DO_PAINEL password CHAVE_MAVEN
```

`Podfile`:

```ruby
platform :ios, '16.0'

source 'https://cdn.cocoapods.org/'
source 'https://maven.aifacial.com.br/ios/specs/'

target 'SeuApp' do
  pod 'AIFacialSDK', '1.5.0'
end
```

```bash
pod install
```

Abra o `.xcworkspace` (não o `.xcodeproj`).

No `Info.plist` do app:
- `NSCameraUsageDescription` (obrigatório);
- para a auditoria em segundo plano: `BGTaskSchedulerPermittedIdentifiers` com
  `br.com.aifacial.sdk.audit`, e `UIBackgroundModes` com `fetch`.

### Uso

```swift
// Na abertura do app (opcional, para a auditoria em segundo plano)
AIFacial.registerBackgroundAudit()

// 1. Configurar
let config = AIFacialConfig(companyId: "ID_DA_EMPRESA", mobileToken: "TOKEN_MOBILE",
                            companyToken: "TOKEN_DA_EMPRESA")
    .liveness(.active)   // opcional: prova de vida
    .audit(true)         // opcional: auditoria
AIFacial.initialize(config)

// 2. Preparar: baixa a IA (1ª vez) e sincroniza as pessoas
AIFacial.prepare(progress: { etapa, percentual in }, completion: { resultado in })

// 3. Reconhecer
sessao = AIFacial.startRecognition(previewView: preview, onError: { erro in }, onResult: { r in
    if r.isNewEvent {
        // r.personId, r.personName, r.eventId → aqui o seu app registra o ponto
    }
})
```

Exemplo completo: [`ios/`](ios).

> No iOS, a auditoria em segundo plano depende do sistema: ela é enviada na hora quando há internet,
> ao abrir o app e oportunisticamente em segundo plano. Se o usuário fechar o app à força, o envio
> espera a próxima abertura. Nenhum reconhecimento se perde.

---

## Resultados do reconhecimento

O SDK devolve um resultado a cada quadro analisado:

| Status | Significado | O que mostrar |
|---|---|---|
| `RECOGNIZED` | pessoa confirmada | nome da pessoa |
| `AMBIGUOUS` | em dúvida entre duas pessoas cadastradas | a dica: "Aproxime-se da câmera", "Olhe de frente", "Melhore a iluminação" ou "Fique parado" |
| `LOW_CONFIDENCE` | parecido com alguém, ainda confirmando | "Aguarde..." |
| `NOT_RECOGNIZED` | rosto desconhecido | "Não reconhecido" |
| `NO_FACE` | nenhum rosto na imagem | "Aproxime seu rosto" |
| `NO_PERSONS` | nenhuma pessoa sincronizada no aparelho | "Sincronize antes" |
| `LIVENESS_CHALLENGE` | prova de vida em andamento | a ação pedida: "Vire à esquerda", "Sorria"... |
| `LIVENESS_FAILED` | prova de vida não confirmada | "Tente de novo" |
| `ERROR` | falha ao processar o quadro | — |

Use o indicador de **evento novo** (`isNewEvent`) para registrar o ponto uma vez só: ele é verdadeiro
apenas no primeiro reconhecimento da pessoa diante da câmera.

---

## Configuração

| Opção | Padrão | Para que serve |
|---|---|---|
| `recognitionThreshold` | `0.85` | distância máxima para reconhecer (menor = mais rigoroso) |
| `confirmationFrames` | `2` | quadros seguidos com a mesma pessoa para confirmar |
| `minMargin` | `0.1` | diferença mínima entre a 1ª e a 2ª pessoa mais parecidas (0 desliga) |
| `ambiguousTimeoutMs` | `3000` | tempo pedindo uma leitura melhor antes de desistir |
| `liveness` | desligada | prova de vida |
| `livenessChallenges` | `3` | quantas ações sortear na prova de vida (1 a 6) |
| `livenessTimeoutMs` | `20000` | tempo total da prova de vida |
| `actionTimeoutMs` | `3000` | tempo para fazer cada ação |
| `rejectWrongAction` | ligado | reprova quem faz outra ação no lugar da pedida |
| `audit` | desligada | auditoria dos reconhecimentos |

---

## Outras operações

| Operação | Para quê |
|---|---|
| `syncPersons` (todas) | atualiza as pessoas do aparelho |
| `syncPersons` (lista de IDs) | baixa só as pessoas informadas |
| `clearPersons` | apaga só as pessoas do aparelho (mantém a IA), para baixar de novo |
| `updateModel` | baixa só a IA (ou uma versão nova dela) |
| `getLocalPersons` | lista as pessoas do aparelho (id, nome e foto), sem internet |
| `startEnrollment` + `AIFacialAdmin` | cadastro de faces pelo celular (exige o login do painel) |
| `clearData` | apaga a IA e as pessoas do aparelho |

---

## Segurança e LGPD

- O SDK é distribuído só em forma compilada; o acesso exige um cliente **ativo** e uma chave válida.
- **Nunca** coloque a chave Maven, os tokens ou a senha do painel no código versionado:
  use arquivos locais fora do Git (`~/.gradle/gradle.properties`, `~/.netrc`, `local.properties`,
  `Local.xcconfig`).
- Fotos e templates faciais são **dados biométricos** (dados pessoais sensíveis pela LGPD):
  informe os funcionários, obtenha o consentimento e defina por quanto tempo as fotos da auditoria
  ficam guardadas.
- Num app de produção, controle quem pode acessar o **cadastro de faces**.

---

© AIFacial · [aifacial.com.br](https://aifacial.com.br)
