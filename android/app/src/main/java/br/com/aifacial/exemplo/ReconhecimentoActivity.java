package br.com.aifacial.exemplo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import br.com.aifacial.sdk.AIFacial;
import br.com.aifacial.sdk.AIFacialException;
import br.com.aifacial.sdk.LivenessChallenge;
import br.com.aifacial.sdk.LivenessFailReason;
import br.com.aifacial.sdk.RecognitionHint;
import br.com.aifacial.sdk.RecognitionListener;
import br.com.aifacial.sdk.RecognitionResult;
import br.com.aifacial.sdk.RecognitionSession;
import br.com.aifacial.sdk.RecognitionStatus;

/**
 * Tela de reconhecimento: o app cliente só fornece o PreviewView e recebe os resultados.
 * O que fazer com a pessoa reconhecida (registrar o ponto, por exemplo) é decisão do app.
 */
public class ReconhecimentoActivity extends AppCompatActivity {

    private TextView lbStatus, lbPessoa;
    private ImageView imgPessoa;
    private ProgressBar barTempo;
    /** Etapa da prova de vida que a barra está mostrando e o tempo total dela (ms). */
    private int etapaBarra = -1;
    private long tempoTotalEtapa = 1;
    private RecognitionSession sessao;
    private String ultimoId;
    private String TAG_LOGCAT = "AIFACIAL";

    private final ActivityResultLauncher<String> pedirCamera =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida) {
                    iniciar();
                } else {
                    Toast.makeText(this, "Permissão da câmera é necessária", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reconhecimento);

        lbStatus = findViewById(R.id.lbStatus);
        lbPessoa = findViewById(R.id.lbPessoa);
        imgPessoa = findViewById(R.id.imgPessoa);
        barTempo = findViewById(R.id.barTempo);
        findViewById(R.id.btGiraCam).setOnClickListener(v -> {
            if (sessao != null) {
                sessao.switchCamera();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciar();
        } else {
            pedirCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciar() {
        PreviewView preview = findViewById(R.id.preview);
        // A sessão é encerrada sozinha quando esta Activity for destruída.
        sessao = AIFacial.startRecognition(this, preview, new RecognitionListener() {
            @Override
            public void onResult(@NonNull RecognitionResult r) {
                mostrar(r);
            }

            @Override
            public void onError(@NonNull AIFacialException error) {
                Toast.makeText(ReconhecimentoActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void mostrar(RecognitionResult r) {
        lbStatus.setTextColor(Color.BLACK);
        if (r.getStatus() != RecognitionStatus.LIVENESS_CHALLENGE) {
            barTempo.setVisibility(View.GONE);
            etapaBarra = -1;
        }
        switch (r.getStatus()) {
            case RECOGNIZED:
                Log.i(TAG_LOGCAT, "Reconhecido: " + r.getPersonName() + " (" + r.getPersonId() + ")\ndistância "
                        + String.format("%.3f", r.getDistance()));

                lbStatus.setBackgroundColor(Color.parseColor("#00FF00"));
                lbStatus.setText("Reconhecido");
                lbPessoa.setText(r.getPersonName() + " (" + r.getPersonId() + ")\ndistância "
                        + String.format("%.3f", r.getDistance()));
                if (!r.getPersonId().equals(ultimoId)) {
                    imgPessoa.setImageBitmap(r.getPhoto());
                    ultimoId = r.getPersonId();
                    // Aqui o app cliente registraria o ponto de r.getPersonId() em r.getTimestamp().
                }
                imgPessoa.setVisibility(View.VISIBLE);
                return;
            case LIVENESS_CHALLENGE:
                // Prova de vida: a pessoa ainda NÃO está confirmada. Só mostra a instrução.
                lbStatus.setBackgroundColor(Color.parseColor("#80C8FF"));
                // Seta: na câmera traseira a imagem não é espelhada, então esquerda/direita da tela invertem.
                boolean frontal = sessao == null || sessao.isFrontCamera();
                lbStatus.setText(instrucao(r.getChallenge(), frontal) + "  (" + r.getChallengeStep()
                        + "/" + r.getChallengeTotal() + ")");
                mostrarTempo(r);
                lbPessoa.setText(r.getPersonName());
                imgPessoa.setVisibility(View.GONE);
                ultimoId = null;
                return;
            case LIVENESS_FAILED:
                lbStatus.setBackgroundColor(Color.parseColor("#FF8080"));
                lbStatus.setText("Prova de vida não confirmada\n" + motivo(r.getFailReason()));
                break;
            case AMBIGUOUS:
                // Em dúvida entre duas pessoas: orienta para uma leitura melhor (1.3.0+).
                lbStatus.setBackgroundColor(Color.parseColor("#FFB347"));
                lbStatus.setText(dica(r.getHint()));
                break;
            case LOW_CONFIDENCE:
                lbStatus.setBackgroundColor(Color.parseColor("#FFCC00"));
                lbStatus.setText("Seria " + r.getPersonName() + "?\n" +
                        "Distância: " + String.format("%.3f", r.getDistance()));
                break;
            case NOT_RECOGNIZED:
                lbStatus.setBackgroundColor(Color.parseColor("#FF8080"));
                lbStatus.setText("Não reconhecido");
                break;
            case NO_FACE:
                lbStatus.setBackgroundColor(Color.parseColor("#80FFFFFF"));
                lbStatus.setText("Aproxime seu rosto");
                break;
            case NO_PERSONS:
                lbStatus.setBackgroundColor(Color.parseColor("#FF8080"));
                lbStatus.setText("Nenhuma pessoa sincronizada");
                break;
            case ERROR:
                lbStatus.setBackgroundColor(Color.parseColor("#FF8080"));
                lbStatus.setText("Erro ao analisar imagem");
                break;
        }
        lbPessoa.setText("");
        imgPessoa.setVisibility(View.GONE);
        ultimoId = null;
    }

    private static String dica(RecognitionHint h) {
        if (h == null) {
            return "Fique parado";
        }
        switch (h) {
            case MOVE_CLOSER:
                return "Aproxime-se da câmera";
            case LOOK_STRAIGHT:
                return "Olhe de frente para a câmera";
            case POOR_LIGHT:
                return "Melhore a iluminação";
            default:
                return "Fique parado";
        }
    }

    /**
     * Texto da ação. Esquerda/direita são da PESSOA; a seta aponta para onde o rosto vai se mover
     * NA TELA: na câmera frontal (imagem espelhada) é o mesmo lado; na traseira, o lado oposto.
     */
    private static String instrucao(LivenessChallenge c, boolean cameraFrontal) {
        if (c == null) {
            return "";
        }
        String setaEsq = cameraFrontal ? "⬅" : "➡";
        String setaDir = cameraFrontal ? "➡" : "⬅";
        switch (c) {
            case TURN_LEFT:
                return setaEsq + " Vire o rosto para a esquerda";
            case TURN_RIGHT:
                return "Vire o rosto para a direita " + setaDir;
            case LOOK_UP:
                return "⬆ Olhe para cima";
            case LOOK_DOWN:
                return "⬇ Olhe para baixo";
            case SMILE:
                return "😀 Sorria";
            case STOP_SMILING:
                return "😐 Pare de sorrir";
            case BLINK:
                return "😉 Pisque os olhos";
            case MOVE_CLOSER:
                return "Aproxime o rosto";
            case MOVE_AWAY:
                return "Afaste o rosto";
            default:
                return "Olhe para a câmera";
        }
    }

    /**
     * Barra regressiva do tempo da ação atual. O total de cada etapa é o tempo restante no primeiro
     * resultado dela (= actionTimeoutMs da configuração). Verde, amarelo e vermelho conforme acaba.
     */
    private void mostrarTempo(RecognitionResult r) {
        long restante = r.getChallengeRemainingMs();
        if (r.getChallengeStep() != etapaBarra) {
            etapaBarra = r.getChallengeStep();
            tempoTotalEtapa = Math.max(1, restante);
        }
        float fracao = Math.min(1f, (float) restante / tempoTotalEtapa);
        int cor = fracao > 0.5f ? Color.parseColor("#2E7D32")
                : fracao > 0.25f ? Color.parseColor("#F9A825")
                : Color.parseColor("#C62828");
        barTempo.setProgressTintList(ColorStateList.valueOf(cor));
        barTempo.setProgress(Math.round(fracao * 1000), true);
        barTempo.setVisibility(View.VISIBLE);
    }

    private static String motivo(LivenessFailReason m) {
        if (m == null) {
            return "";
        }
        switch (m) {
            case ACTION_TIMEOUT:
                return "Tempo esgotado para a ação";
            case WRONG_ACTION:
                return "Foi feita outra ação";
            case FACE_LOST:
                return "O rosto saiu da câmera";
            case FACE_CHANGED:
                return "O rosto mudou";
            case OTHER_PERSON:
                return "Outra pessoa no final";
            default:
                return "Tempo esgotado";
        }
    }
}
