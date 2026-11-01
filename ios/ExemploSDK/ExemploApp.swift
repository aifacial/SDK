import AIFacialSDK
import SwiftUI

@main
struct ExemploApp: App {

    init() {
        // Opcional: envio da auditoria em segundo plano. Precisa ser chamado na abertura do app
        // (e o Info.plist precisa de BGTaskSchedulerPermittedIdentifiers e UIBackgroundModes/fetch).
        AIFacial.registerBackgroundAudit()
    }

    var body: some Scene {
        WindowGroup {
            NavigationView {
                PrincipalView()
            }
            .navigationViewStyle(.stack)
        }
    }
}

/// Guarda o login do painel enquanto o app está aberto (nada é gravado no aparelho).
enum Sessao {
    static var admin: AIFacialAdmin?
}

/// Valores de teste do Configuracao/Local.xcconfig (veja o README). "COLE_AQUI..." conta como vazio.
enum ValoresDeTeste {
    static func valor(_ chave: String) -> String {
        let v = (Bundle.main.object(forInfoDictionaryKey: chave) as? String ?? "")
            .trimmingCharacters(in: .whitespaces)
        return v.hasPrefix("COLE_AQUI") ? "" : v
    }

    static var empresa: String { valor("AIFacialCompanyId") }
    static var token: String { valor("AIFacialMobileToken") }
    static var tokenEmpresa: String { valor("AIFacialCompanyToken") }
    static var usuario: String { valor("AIFacialLoginUser") }
    static var senha: String { valor("AIFacialLoginPassword") }
}
