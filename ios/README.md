# AIFacial — App de exemplo do SDK iOS

App iOS (SwiftUI) de referência para o **SDK AIFacial**: reconhecimento facial **offline**.
É o mesmo app do `android/ExemploSDK`, tela por tela.

O exemplo usa o SDK **exatamente como um app cliente**: a biblioteca é baixada do repositório
CocoaPods da AIFacial com uma linha no `Podfile`, sem acesso ao código do SDK.

---

## O que o exemplo mostra

| Recurso | Onde ver |
|---|---|
| **Preparar**: baixa o modelo de IA (na 1ª vez) e sincroniza as pessoas da empresa | tela inicial |
| **Só baixar a IA**: baixa (ou atualiza) o modelo sem mexer nas pessoas do aparelho | botão "Só baixar a IA" |
| **Sincronizar só algumas pessoas**: informe os IDs e baixe só os templates delas | campo "IDs das pessoas" |
| **Apagar as pessoas do aparelho** (mantém a IA), para baixar de novo | botão "Apagar pessoas do aparelho" |
| **Reconhecimento facial** offline, com a pessoa reconhecida, a foto e a distância | `ReconhecimentoView` |
| **Em dúvida entre duas pessoas**: orienta "aproxime-se", "olhe de frente"... (`.ambiguous`) | `ReconhecimentoView` (laranja) |
| **Prova de vida** *(opcional)*: pede ações sorteadas (virar o rosto, piscar, sorrir...) contra fotos e vídeos | ligue "Prova de vida" |
| **Auditoria** *(opcional)*: foto de cada reconhecimento enviada ao servidor automaticamente | ligue "Auditoria" |
| **Cadastro de faces** pelo celular: login do painel → pessoa **deste aparelho** → captura guiada → envio | `PessoasView`, `CadastroFaceView` |

---

## Requisitos

- Mac com Xcode e CocoaPods
- iPhone com **iOS 16** ou superior (o SDK aceita iOS 15.5; o exemplo usa 16). O simulador não
  tem câmera: o reconhecimento só pode ser testado no aparelho.
- Uma conta de desenvolvedor Apple no Xcode (a gratuita serve para instalar no seu iPhone)
- Acesso ao painel AIFacial do seu cliente:
  - **login** do painel;
  - **chave Maven** (painel → **Credenciais** → **Gerar chave Maven**);
  - **ID da empresa** e **token mobile** (painel → **Credenciais**);
  - **token da empresa** (painel → **Empresas**, ou `companytoken` da API `company_add`/`company_list`).

---

## Configuração

### 1. Acesso ao repositório — `~/.netrc`

```
machine maven.aifacial.com.br login LOGIN_DO_PAINEL password CHAVE_MAVEN
```

```bash
chmod 600 ~/.netrc
```

A chave Maven aparece **uma única vez**, quando é gerada. Se não tiver mais, gere uma nova
(a anterior deixa de valer). É a mesma chave usada no Android.

### 2. Valores pré-preenchidos e assinatura — `Configuracao/Local.xcconfig`

```bash
cp Configuracao/Local.xcconfig.exemplo Configuracao/Local.xcconfig
```

```
DEVELOPMENT_TEAM = SEU_TEAM_ID
AIFACIAL_COMPANY_ID = ID_DA_EMPRESA
AIFACIAL_MOBILE_TOKEN = TOKEN_MOBILE
AIFACIAL_COMPANY_TOKEN = TOKEN_DA_EMPRESA
AIFACIAL_LOGIN_USER = LOGIN_DO_PAINEL
AIFACIAL_LOGIN_PASSWORD = SENHA_DO_PAINEL
```

- O Team ID aparece no Xcode em **Settings → Accounts** (ou escolha o time na aba
  *Signing & Capabilities* do projeto).
- Com uma conta gratuita, troque também o `PRODUCT_BUNDLE_IDENTIFIER` por um só seu.
- Valores em branco (ou começando com `COLE_AQUI`) ficam vazios no app, e dá para digitar na tela.
- Nos arquivos `.xcconfig`, `//` começa um comentário: evite `//` nos valores.

---

## Compilar e instalar

```bash
ruby scripts/gerar_projeto.rb
pod install
open ExemploSDK.xcworkspace
```

No Xcode, escolha o seu iPhone e toque em **Run**. Na primeira vez, o iPhone pede para confiar no
desenvolvedor: **Ajustes → Geral → VPN e Gerenciamento de Dispositivos**.

---

## Como usar o app

1. Confira o **ID da empresa**, o **token mobile** e o **token da empresa** na tela inicial.
2. Toque em **Preparar**: baixa a IA (só na primeira vez) e sincroniza as pessoas.
   Ao terminar, aparece *"IA pronta · N pessoas no aparelho"*.
3. *(Opcional)* Para trabalhar só com algumas pessoas: em vez de **Preparar**, toque em
   **Só baixar a IA** (ou em **Apagar pessoas do aparelho**, se já preparou), digite os IDs
   (separados por vírgula) e toque em **Sincronizar só estas pessoas**.
4. Toque em **Reconhecimento facial** e permita o uso da câmera.
   - Verde: **Reconhecido**, com o nome, o ID e a distância (menor = mais parecido).
   - Amarelo: parecido com alguém, mas ainda não confirmado.
5. *(Opcional)* Ligue **Prova de vida** e/ou **Auditoria** antes de abrir o reconhecimento.
6. **Cadastro de faces**: entre com o login do painel, escolha a pessoa (a lista mostra as pessoas
   sincronizadas neste aparelho, com busca por nome ou ID), posicione o rosto
   (o app orienta e captura sozinho) e toque em **Enviar**.

---

## O código em 3 passos

A dependência (`Podfile`):

```ruby
source 'https://cdn.cocoapods.org/'
source 'https://maven.aifacial.com.br/ios/specs/'

pod 'AIFacialSDK', '1.5.0'
```

```swift
import AIFacialSDK

// 1. Configurar
AIFacial.initialize(AIFacialConfig(companyId: empresa, mobileToken: tokenMobile, companyToken: tokenEmpresa)
    .liveness(.active)   // opcional
    .audit(true))        // opcional

// 2. Preparar
let pessoas = try await AIFacial.prepare()

// 3. Reconhecer (preview é uma AIFacialPreviewView na tela)
let sessao = AIFacial.startRecognition(previewView: preview) { resultado in
    if resultado.isNewEvent {
        // primeira vez que esta pessoa é reconhecida diante da câmera:
        // resultado.personId, resultado.timestamp, resultado.eventId
        // → aqui o seu app registra o ponto
    }
}
```

---

## Estrutura

```
ExemploSDK/
├── ExemploSDK/
│   ├── ExemploApp.swift          abertura do app, login guardado (Sessao), valores de teste
│   ├── PrincipalView.swift       configuração, preparar, opções, login do painel
│   ├── ReconhecimentoView.swift  câmera de reconhecimento (status, prova de vida)
│   ├── PessoasView.swift         pessoas deste aparelho, com foto e busca
│   ├── CadastroFaceView.swift    captura guiada e envio da face
│   ├── CameraPreview.swift       AIFacialPreviewView no SwiftUI, permissão da câmera
│   └── Info.plist                permissão da câmera, auditoria em segundo plano
├── Configuracao/                 xcconfig (Local.xcconfig: time, empresa, token, login)
├── Podfile                       repositório da AIFacial e versão do SDK (AIFACIAL_SDK)
└── scripts/gerar_projeto.rb      gera o ExemploSDK.xcodeproj
```

Para usar outra versão do SDK, altere `AIFACIAL_SDK` no `Podfile` e rode `pod install --repo-update`.

Para testar o app contra outro servidor (homologação), defina a variável de ambiente
`AIFACIAL_SERVER_URL` em *Product → Scheme → Edit Scheme → Run → Environment Variables*.

---

## Problemas comuns

| Sintoma | Solução |
|---|---|
| `pod install`: *Unable to find a specification for AIFacialSDK* | confira o `~/.netrc` (login e chave Maven) e rode `pod install --repo-update` |
| `pod install`: erro 401 | login ou chave Maven errados, ou cliente inativo: gere uma nova chave |
| Xcode: *Signing requires a development team* | preencha `DEVELOPMENT_TEAM` no `Local.xcconfig` |
| *"Empresa ou token mobile inválido"* / *"Token is invalid"* | confira o ID da empresa e o token mobile (Credenciais) e o token **dessa** empresa (Empresas) |
| *"Toque em Preparar primeiro"* | a IA ainda não foi baixada neste aparelho |
| *"Nenhuma pessoa sincronizada"* | não há pessoas com face cadastrada na empresa |
| *"Usuário ou senha inválidos"* / *"Sessão expirada"* | refaça o login do painel no Cadastro de faces |
| Simulador: erro de link do ML Kit em arm64 | o ML Kit só tem x86_64 para o simulador: use um iPhone |

---

## Segurança

- O `Configuracao/Local.xcconfig` e o `~/.netrc` podem conter credenciais reais (chave Maven,
  token, senha do painel). O `Local.xcconfig` fica fora do Git (`.gitignore`).
- Fotos e templates faciais são **dados biométricos** (LGPD): informe as pessoas e obtenha o
  consentimento antes de usar o reconhecimento e a auditoria.
- Num app de produção, controle quem pode acessar o **Cadastro de faces**.

---

© AIFacial · [aifacial.com.br](https://aifacial.com.br)
