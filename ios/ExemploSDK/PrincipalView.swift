import AIFacialSDK
import SwiftUI

/// Tela inicial do app de exemplo: configura o SDK, prepara (baixa a IA e sincroniza)
/// e abre o reconhecimento. Mostra o mínimo que um app cliente precisa fazer.
struct PrincipalView: View {

    @AppStorage("empresa") private var empresa = ValoresDeTeste.empresa
    @AppStorage("token") private var token = ValoresDeTeste.token
    @AppStorage("tokenEmpresa") private var tokenEmpresa = ValoresDeTeste.tokenEmpresa
    @AppStorage("liveness") private var provaDeVida = false
    @AppStorage("auditoria") private var auditoria = false
    @AppStorage("loginUsuario") private var ultimoUsuario = ""

    @State private var ids = ""
    @State private var status = ""
    @State private var auditoriaPendente = 0
    @State private var ocupado = false
    @State private var progresso: Double?
    @State private var aviso: String?

    @State private var abrirReconhecimento = false
    @State private var abrirPessoas = false
    @State private var pedirLogin = false
    @State private var usuario = ""
    @State private var senha = ""

    var body: some View {
        Form {
            Section {
                TextField("ex.: C44BF9", text: $empresa)
                    .textInputAutocapitalization(.characters)
                    .disableAutocorrection(true)
            } header: {
                Text("ID da empresa")
            }
            Section {
                TextField("Token mobile", text: $token)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
            } header: {
                Text("Token mobile (painel → Credenciais)")
            }
            Section {
                TextField("Token da empresa", text: $tokenEmpresa)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
            } header: {
                Text("Token da empresa (painel → Empresas)")
            }
            Section {
                Toggle("Prova de vida (pedir ações)", isOn: $provaDeVida)
                Toggle("Auditoria (enviar foto de cada reconhecimento)", isOn: $auditoria)
                Text("Auditoria pendente de envio: \(auditoriaPendente)")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }

            Section {
                Button("Preparar (baixar IA e sincronizar)") { preparar(.tudo) }
                Button("Só baixar a IA") { preparar(.modelo) }
                Button("Só sincronizar pessoas") { preparar(.pessoas) }
                TextField("IDs das pessoas, separados por vírgula", text: $ids)
                    .keyboardType(.numbersAndPunctuation)
                    .disableAutocorrection(true)
                Button("Sincronizar só estas pessoas", action: sincronizarLista)
                Button("Apagar pessoas do aparelho (manter IA)", action: apagarPessoas)
            }
            .disabled(ocupado)

            Section {
                if let progresso {
                    ProgressView(value: progresso, total: 100)
                } else if ocupado {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                }
                Text(status)
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
            }

            Section {
                Button("Reconhecimento facial", action: reconhecer)
                Button("Cadastro de faces (login do painel)", action: cadastro)
            }
            .disabled(ocupado)

            Section {
                Button("Apagar dados do SDK neste aparelho", role: .destructive) {
                    if configurar() {
                        AIFacial.clearData()
                        status = "Dados apagados"
                    }
                }
            } footer: {
                Text("SDK AIFacial \(AIFacial.version)")
                    .frame(maxWidth: .infinity)
            }
        }
        .navigationTitle("AIFacial Exemplo")
        .background(
            Group {
                NavigationLink(destination: ReconhecimentoView(), isActive: $abrirReconhecimento) { EmptyView() }
                NavigationLink(destination: PessoasView(), isActive: $abrirPessoas) { EmptyView() }
            }
            .hidden()
        )
        .alert(aviso ?? "", isPresented: Binding(get: { aviso != nil }, set: { if !$0 { aviso = nil } })) {
            Button("OK", role: .cancel) {}
        }
        .alert("Login do painel", isPresented: $pedirLogin) {
            TextField("Usuário", text: $usuario)
                .textInputAutocapitalization(.never)
            SecureField("Senha", text: $senha)
            Button("Cancelar", role: .cancel) {}
            Button("Entrar", action: entrar)
        }
        .onAppear {
            if !empresa.isEmpty && configurar() {
                mostrarSituacao()
            }
        }
    }

    /// Lê os campos e chama AIFacial.initialize(). Num app real isso fica na abertura do app.
    @discardableResult
    private func configurar() -> Bool {
        let e = empresa.trimmingCharacters(in: .whitespaces)
        let t = token.trimmingCharacters(in: .whitespaces)
        let te = tokenEmpresa.trimmingCharacters(in: .whitespaces)
        if e.isEmpty || t.isEmpty || te.isEmpty {
            aviso = "Informe a empresa e os dois tokens"
            return false
        }
        var config = AIFacialConfig(companyId: e, mobileToken: t, companyToken: te)
            .liveness(provaDeVida ? .active : .none)
            .livenessChallenges(2)   // 2 ações sorteadas + "olhe para a câmera" (padrão do SDK: 3)
            .audit(auditoria)
        // Só para testes: outro servidor pela variável de ambiente AIFACIAL_SERVER_URL
        // (Xcode → Product → Scheme → Edit Scheme → Run → Environment Variables).
        if let url = ProcessInfo.processInfo.environment["AIFACIAL_SERVER_URL"], !url.isEmpty {
            config = config.serverUrl(url)
        }
        AIFacial.initialize(config)
        return true
    }

    private func mostrarSituacao() {
        auditoriaPendente = AIFacial.pendingAuditCount
        status = (AIFacial.isModelReady ? "IA pronta" : "IA não baixada")
            + " · \(AIFacial.personCount) pessoas no aparelho"
    }

    private enum Preparo { case tudo, modelo, pessoas }

    private func preparar(_ tipo: Preparo) {
        guard configurar() else {
            return
        }
        ocupado = true
        progresso = nil
        let aoProgredir: @MainActor (PrepareStep, Int) -> Void = { etapa, porcento in
            progresso = porcento < 0 ? nil : Double(porcento)
            status = etapa == .downloadingModel ? "Baixando IA: \(porcento)%" : "Sincronizando pessoas: \(porcento)%"
        }
        let aoTerminar: @MainActor (Result<Int, AIFacialError>) -> Void = { resultado in
            ocupado = false
            progresso = nil
            switch resultado {
            case .success:
                mostrarSituacao()
            case .failure(let erro):
                status = "Erro (\(erro.code.rawValue)): \(erro.message)"
            }
        }
        switch tipo {
        case .tudo:
            AIFacial.prepare(progress: aoProgredir, completion: aoTerminar)
        case .modelo:
            // Baixa (ou atualiza) só o modelo de IA; as pessoas do aparelho não mudam.
            AIFacial.updateModel(progress: aoProgredir, completion: aoTerminar)
        case .pessoas:
            AIFacial.syncPersons(progress: aoProgredir, completion: aoTerminar)
        }
    }

    private func sincronizarLista() {
        guard configurar() else {
            return
        }
        let lista = ids.components(separatedBy: CharacterSet(charactersIn: ",; \n\t"))
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
        if lista.isEmpty {
            aviso = "Informe os IDs das pessoas"
            return
        }
        ocupado = true
        status = "Sincronizando \(lista.count) pessoa(s)..."
        AIFacial.syncPersons(ids: lista) { resultado in
            ocupado = false
            switch resultado {
            case .success(let r):
                mostrarSituacao()
                var msg = "\(r.received) pessoa(s) baixada(s)"
                if !r.notFound.isEmpty {
                    msg += "\nSem face ou inexistentes: " + r.notFound.joined(separator: ", ")
                }
                if !r.deleted.isEmpty {
                    msg += "\nApagadas do aparelho (excluídas): " + r.deleted.joined(separator: ", ")
                }
                aviso = msg
            case .failure(let erro):
                status = "Erro (\(erro.code.rawValue)): \(erro.message)"
            }
        }
    }

    private func apagarPessoas() {
        guard configurar() else {
            return
        }
        AIFacial.clearPersons()
        status = "Pessoas apagadas do aparelho (IA mantida)"
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { mostrarSituacao() }
    }

    private func reconhecer() {
        guard configurar() else {
            return
        }
        guard AIFacial.isModelReady else {
            aviso = "Toque em Preparar primeiro"
            return
        }
        abrirReconhecimento = true
    }

    private func cadastro() {
        guard configurar() else {
            return
        }
        guard AIFacial.isModelReady else {
            aviso = "Toque em Preparar primeiro"
            return
        }
        if Sessao.admin != nil {
            abrirPessoas = true
        } else {
            // Preenche com o login do Local.xcconfig ou com o último usuário usado.
            usuario = ValoresDeTeste.usuario.isEmpty ? ultimoUsuario : ValoresDeTeste.usuario
            senha = ValoresDeTeste.senha
            pedirLogin = true
        }
    }

    /// Login do painel do cliente (a api/v1 exige). Num app real, controle quem pode acessar.
    private func entrar() {
        status = "Entrando..."
        AIFacialAdmin.login(username: usuario, password: senha) { resultado in
            switch resultado {
            case .success(let admin):
                Sessao.admin = admin
                ultimoUsuario = usuario
                mostrarSituacao()
                abrirPessoas = true
            case .failure(let erro):
                status = "Login: \(erro.message)"
            }
        }
    }
}
