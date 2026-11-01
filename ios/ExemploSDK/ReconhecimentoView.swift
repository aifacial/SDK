import AIFacialSDK
import SwiftUI

/// Tela de reconhecimento: o app cliente só fornece a AIFacialPreviewView e recebe os resultados.
/// O que fazer com a pessoa reconhecida (registrar o ponto, por exemplo) é decisão do app.
struct ReconhecimentoView: View {

    @Environment(\.dismiss) private var fechar
    @State private var sessao: RecognitionSession?
    @State private var texto = "Iniciando..."
    @State private var cor = Color.black.opacity(0.5)
    @State private var corTexto = Color.white
    @State private var pessoa = ""
    @State private var foto: UIImage?
    @State private var ultimoId: String?
    @State private var erro: String?
    /// Tempo restante da ação da prova de vida (0 a 1) e a etapa que a barra está mostrando.
    @State private var tempo: Double?
    @State private var etapaBarra = -1
    @State private var tempoTotalEtapa = 1

    var body: some View {
        ZStack {
            CameraPreview { preview in
                pedirPermissaoDaCamera { concedida in
                    if concedida {
                        iniciar(preview)
                    } else {
                        erro = "Permissão da câmera é necessária"
                    }
                }
            }
            .ignoresSafeArea()

            MolduraRosto()

            VStack {
                Text(texto)
                    .font(.system(size: 22))
                    .multilineTextAlignment(.center)
                    .foregroundColor(corTexto)
                    .padding(10)
                    .background(cor)
                    .padding(.top, 30)
                if let tempo {
                    // Barra regressiva do tempo da ação: verde, amarelo e vermelho conforme acaba.
                    ProgressView(value: tempo)
                        .progressViewStyle(.linear)
                        .tint(tempo > 0.5 ? .green : (tempo > 0.25 ? .yellow : .red))
                        .frame(width: 260)
                        .scaleEffect(x: 1, y: 3, anchor: .center)
                        .animation(.linear(duration: 0.1), value: tempo)
                }
                HStack {
                    Spacer()
                    BotaoTrocarCamera { sessao?.switchCamera() }
                        .padding(.trailing, 20)
                        .padding(.top, 16)
                }
                Spacer()
                HStack(spacing: 10) {
                    if let foto {
                        Image(uiImage: foto)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 60, height: 60)
                            .clipped()
                    }
                    Text(pessoa)
                        .font(.system(size: 16))
                        .foregroundColor(.black)
                    Spacer()
                }
                .padding(10)
                .frame(height: 80)
                .background(Color(white: 0.77))
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .onDisappear { sessao?.stop() }
        .alert(erro ?? "", isPresented: Binding(get: { erro != nil }, set: { if !$0 { erro = nil; fechar() } })) {
            Button("OK", role: .cancel) {}
        }
    }

    private func iniciar(_ preview: AIFacialPreviewView) {
        // A sessão é encerrada sozinha quando a preview for destruída (ou com stop()).
        sessao = AIFacial.startRecognition(previewView: preview, onError: { e in
            erro = e.message
        }, onResult: mostrar)
    }

    private func mostrar(_ r: RecognitionResult) {
        corTexto = .black
        if r.status != .livenessChallenge {
            tempo = nil
            etapaBarra = -1
        }
        switch r.status {
        case .recognized:
            let distancia = String(format: "%.3f", r.distance)
            print("AIFACIAL Reconhecido: \(r.personName ?? "") (\(r.personId ?? ""))\ndistância \(distancia)")
            cor = Color(red: 0, green: 1, blue: 0)
            texto = "Reconhecido"
            pessoa = "\(r.personName ?? "") (\(r.personId ?? ""))\ndistância \(distancia)"
            if r.personId != ultimoId {
                foto = r.photo
                ultimoId = r.personId
            }
            if r.isNewEvent {
                // Aqui o app cliente registraria o ponto de r.personId em r.timestamp
                // (guardando r.eventId, o mesmo id da auditoria).
            }
            return
        case .livenessChallenge:
            // Prova de vida: a pessoa ainda NÃO está confirmada. Só mostra a instrução.
            cor = Color(red: 0.5, green: 0.78, blue: 1)
            // Seta: na câmera traseira a imagem não é espelhada, então esquerda/direita da tela invertem.
            let frontal = sessao?.isFrontCamera ?? true
            texto = "\(instrucao(r.challenge, frontal))  (\(r.challengeStep)/\(r.challengeTotal))"
            mostrarTempo(r)
            pessoa = r.personName ?? ""
            foto = nil
            ultimoId = nil
            return
        case .livenessFailed:
            cor = Color(red: 1, green: 0.5, blue: 0.5)
            texto = "Prova de vida não confirmada\n\(motivo(r.failReason))"
        case .ambiguous:
            // Em dúvida entre duas pessoas: orienta para uma leitura melhor.
            cor = Color(red: 1, green: 0.7, blue: 0.28)
            texto = dica(r.hint)
        case .lowConfidence:
            cor = Color(red: 1, green: 0.8, blue: 0)
            texto = "Seria \(r.personName ?? "")?\nDistância: \(String(format: "%.3f", r.distance))"
        case .notRecognized:
            cor = Color(red: 1, green: 0.5, blue: 0.5)
            texto = "Não reconhecido"
        case .noFace:
            cor = Color.white.opacity(0.5)
            texto = "Aproxime seu rosto"
        case .noPersons:
            cor = Color(red: 1, green: 0.5, blue: 0.5)
            texto = "Nenhuma pessoa sincronizada"
        case .error:
            cor = Color(red: 1, green: 0.5, blue: 0.5)
            texto = "Erro ao analisar imagem"
        @unknown default:
            cor = Color.white.opacity(0.5)
            texto = ""
        }
        pessoa = ""
        foto = nil
        ultimoId = nil
    }

    private func dica(_ h: RecognitionHint?) -> String {
        switch h {
        case .moveCloser:
            return "Aproxime-se da câmera"
        case .lookStraight:
            return "Olhe de frente para a câmera"
        case .poorLight:
            return "Melhore a iluminação"
        default:
            return "Fique parado"
        }
    }

    /// Barra regressiva: o total de cada etapa é o tempo restante no primeiro resultado dela
    /// (= actionTimeoutMs da configuração).
    private func mostrarTempo(_ r: RecognitionResult) {
        if r.challengeStep != etapaBarra {
            etapaBarra = r.challengeStep
            tempoTotalEtapa = max(1, r.challengeRemainingMs)
        }
        tempo = min(1, Double(r.challengeRemainingMs) / Double(tempoTotalEtapa))
    }

    /// Texto da ação. Esquerda/direita são da PESSOA; a seta aponta para onde o rosto vai se mover
    /// NA TELA: na câmera frontal (imagem espelhada) é o mesmo lado; na traseira, o lado oposto.
    private func instrucao(_ c: LivenessChallenge?, _ cameraFrontal: Bool) -> String {
        let setaEsq = cameraFrontal ? "⬅" : "➡"
        let setaDir = cameraFrontal ? "➡" : "⬅"
        switch c {
        case .turnLeft:
            return "\(setaEsq) Vire o rosto para a esquerda"
        case .turnRight:
            return "Vire o rosto para a direita \(setaDir)"
        case .lookUp:
            return "⬆ Olhe para cima"
        case .lookDown:
            return "⬇ Olhe para baixo"
        case .smile:
            return "😀 Sorria"
        case .stopSmiling:
            return "😐 Pare de sorrir"
        case .blink:
            return "😉 Pisque os olhos"
        case .moveCloser:
            return "Aproxime o rosto"
        case .moveAway:
            return "Afaste o rosto"
        case .lookStraight:
            return "Olhe para a câmera"
        default:
            return ""
        }
    }

    private func motivo(_ m: LivenessFailReason?) -> String {
        switch m {
        case .actionTimeout:
            return "Tempo esgotado para a ação"
        case .wrongAction:
            return "Foi feita outra ação"
        case .faceLost:
            return "O rosto saiu da câmera"
        case .faceChanged:
            return "O rosto mudou"
        case .otherPerson:
            return "Outra pessoa no final"
        case .timeout:
            return "Tempo esgotado"
        default:
            return ""
        }
    }
}
