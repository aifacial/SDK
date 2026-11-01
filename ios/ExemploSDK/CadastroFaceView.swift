import AIFacialSDK
import SwiftUI

/// Cadastro de face pelo celular: o SDK orienta a pessoa e captura sozinho quando a foto
/// está boa; o app confirma e envia para o servidor (api/v1/person_update).
struct CadastroFaceView: View {

    let pessoa: PersonInfo
    let aoCadastrar: () -> Void

    @Environment(\.dismiss) private var fechar
    @State private var sessao: EnrollmentSession?
    @State private var captura: FaceCapture?
    @State private var instrucaoAtual = "Iniciando..."
    @State private var confirma = "Usar esta foto?"
    @State private var enviando = false
    @State private var mensagem: String?
    @State private var concluido = false

    var body: some View {
        ZStack {
            CameraPreview { preview in
                pedirPermissaoDaCamera { concedida in
                    if concedida {
                        iniciar(preview)
                    } else {
                        mensagem = "Permissão da câmera é necessária"
                    }
                }
            }
            .ignoresSafeArea()

            MolduraRosto()

            VStack(spacing: 0) {
                Text(pessoa.name + (pessoa.hasFace ? "\n(a face atual será substituída)" : ""))
                    .font(.system(size: 18))
                    .multilineTextAlignment(.center)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(10)
                    .background(Color.black.opacity(0.7))
                Text(instrucaoAtual)
                    .font(.system(size: 22))
                    .foregroundColor(.white)
                    .padding(10)
                    .background(Color.black.opacity(0.5))
                    .padding(.top, 16)
                HStack {
                    Spacer()
                    BotaoTrocarCamera { sessao?.switchCamera() }
                        .padding(.trailing, 20)
                        .padding(.top, 16)
                }
                Spacer()
                if let captura {
                    painelConfirma(captura)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .onDisappear { sessao?.stop() }
        .alert(mensagem ?? "", isPresented: Binding(get: { mensagem != nil }, set: { if !$0 { mensagem = nil; fechar() } })) {
            Button("OK", role: .cancel) {}
        }
    }

    /// Painel de confirmação: aparece depois da captura.
    private func painelConfirma(_ c: FaceCapture) -> some View {
        VStack(spacing: 8) {
            Image(uiImage: c.face)
                .resizable()
                .scaledToFill()
                .frame(width: 160, height: 160)
                .clipped()
            Text(confirma).font(.system(size: 16)).multilineTextAlignment(.center)
            if enviando {
                ProgressView()
            }
            HStack {
                Button("Tirar outra", action: tirarOutra)
                    .frame(maxWidth: .infinity)
                Button("Enviar", action: enviar)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(enviando)
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(Color.white.opacity(0.94))
    }

    private func iniciar(_ preview: AIFacialPreviewView) {
        guard Sessao.admin != nil else {
            fechar()
            return
        }
        sessao = AIFacial.startEnrollment(previewView: preview, onFeedback: { f in
            instrucaoAtual = instrucao(f)
        }, onCaptured: { c in
            captura = c
            instrucaoAtual = "Foto capturada"
            confirma = "Usar esta foto?"
        }, onError: { e in
            mensagem = e.message
        })
    }

    private func tirarOutra() {
        captura = nil
        sessao?.retake()
    }

    private func enviar() {
        guard let captura, let admin = Sessao.admin else {
            return
        }
        enviando = true
        confirma = "Enviando..."
        admin.updateFace(person: pessoa, capture: captura) { resultado in
            enviando = false
            switch resultado {
            case .success:
                aoCadastrar()
                mensagem = "Face de \(pessoa.name) cadastrada"
            case .failure(let e):
                confirma = "Erro: \(e.message)"
            }
        }
    }

    private func instrucao(_ f: EnrollmentFeedback) -> String {
        switch f {
        case .noFace:
            return "Posicione o rosto na moldura"
        case .multipleFaces:
            return "Só uma pessoa na imagem"
        case .moveCloser:
            return "Aproxime-se"
        case .centerFace:
            return "Centralize o rosto"
        case .lookStraight:
            return "Olhe de frente para a câmera"
        case .openEyes:
            return "Mantenha os olhos abertos"
        case .poorLight:
            return "Melhore a iluminação"
        default:
            return "Fique parado..."
        }
    }
}
