import AIFacialSDK
import AVFoundation
import SwiftUI

/// Mostra a câmera do SDK numa tela SwiftUI. `aoCriar` recebe a AIFacialPreviewView para
/// iniciar o reconhecimento ou o cadastro.
struct CameraPreview: UIViewRepresentable {

    let aoCriar: (AIFacialPreviewView) -> Void

    func makeUIView(context: Context) -> AIFacialPreviewView {
        let view = AIFacialPreviewView()
        DispatchQueue.main.async { aoCriar(view) }
        return view
    }

    func updateUIView(_ uiView: AIFacialPreviewView, context: Context) {
    }
}

/// Pede a permissão da câmera (o texto do pedido está em NSCameraUsageDescription, no Info.plist).
@MainActor
func pedirPermissaoDaCamera(_ concluir: @escaping @MainActor (Bool) -> Void) {
    switch AVCaptureDevice.authorizationStatus(for: .video) {
    case .authorized:
        concluir(true)
    case .notDetermined:
        AVCaptureDevice.requestAccess(for: .video) { concedida in
            DispatchQueue.main.async { concluir(concedida) }
        }
    default:
        concluir(false)
    }
}

/// Botão redondo de trocar a câmera.
struct BotaoTrocarCamera: View {
    let acao: () -> Void

    var body: some View {
        Button(action: acao) {
            Image(systemName: "arrow.triangle.2.circlepath.camera")
                .font(.system(size: 20))
                .foregroundColor(.black)
                .frame(width: 48, height: 48)
                .background(Circle().fill(Color.white.opacity(0.8)))
        }
        .accessibilityLabel("Trocar câmera")
    }
}

/// Moldura do rosto (a mesma imagem do app Android).
struct MolduraRosto: View {
    var body: some View {
        GeometryReader { g in
            Image("rostoverde")
                .resizable()
                .scaledToFit()
                .frame(width: g.size.width * 0.6)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
