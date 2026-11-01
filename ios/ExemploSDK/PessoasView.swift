import AIFacialSDK
import SwiftUI

/// Lista as pessoas sincronizadas NESTE APARELHO (AIFacial.getLocalPersons, sem internet)
/// e abre o cadastro de face da pessoa escolhida. O envio usa o login do painel (Sessao.admin).
struct PessoasView: View {

    @State private var todas: [PersonInfo] = []
    @State private var busca = ""
    @State private var erro: String?

    private var visiveis: [PersonInfo] {
        let b = normalizar(busca)
        return b.isEmpty ? todas : todas.filter { normalizar($0.name).contains(b) || $0.id.contains(b) }
    }

    var body: some View {
        List {
            Section {
                ForEach(visiveis) { p in
                    NavigationLink(destination: CadastroFaceView(pessoa: p, aoCadastrar: carregar)) {
                        Linha(pessoa: p)
                    }
                }
            } header: {
                Text(todas.isEmpty
                     ? "Nenhuma pessoa neste aparelho. Sincronize antes."
                     : "\(visiveis.count) de \(todas.count) pessoa(s) neste aparelho")
            }
        }
        .listStyle(.plain)
        // Filtra enquanto digita (nome ou ID, sem diferenciar acentos/maiúsculas).
        .searchable(text: $busca, placement: .navigationBarDrawer(displayMode: .always),
                    prompt: "Buscar por nome ou ID")
        .navigationTitle("Pessoas neste aparelho")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: carregar)
        .alert(erro ?? "", isPresented: Binding(get: { erro != nil }, set: { if !$0 { erro = nil } })) {
            Button("OK", role: .cancel) {}
        }
    }

    private func carregar() {
        AIFacial.getLocalPersons { resultado in
            switch resultado {
            case .success(let pessoas):
                todas = pessoas
            case .failure(let e):
                erro = e.message
            }
        }
    }

    private func normalizar(_ s: String) -> String {
        s.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .trimmingCharacters(in: .whitespaces)
    }

    private struct Linha: View {
        let pessoa: PersonInfo

        var body: some View {
            HStack(spacing: 12) {
                Group {
                    if let foto = pessoa.photo {
                        Image(uiImage: foto).resizable().scaledToFill()
                    } else {
                        Color(white: 0.93)
                    }
                }
                .frame(width: 56, height: 56)
                .clipped()
                VStack(alignment: .leading) {
                    Text(pessoa.name).font(.system(size: 17))
                    Text("ID \(pessoa.id)").font(.system(size: 13)).foregroundColor(.secondary)
                }
            }
        }
    }
}
