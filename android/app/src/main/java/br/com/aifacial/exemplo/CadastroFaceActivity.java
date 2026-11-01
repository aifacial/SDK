package br.com.aifacial.exemplo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import br.com.aifacial.sdk.AIFacial;
import br.com.aifacial.sdk.AIFacialCallback;
import br.com.aifacial.sdk.AIFacialException;
import br.com.aifacial.sdk.EnrollmentFeedback;
import br.com.aifacial.sdk.EnrollmentListener;
import br.com.aifacial.sdk.EnrollmentSession;
import br.com.aifacial.sdk.FaceCapture;
import br.com.aifacial.sdk.PersonInfo;

/**
 * Cadastro de face pelo celular: o SDK orienta a pessoa e captura sozinho quando a foto
 * está boa; o app confirma e envia para o servidor (api/v1/person_update).
 */
public class CadastroFaceActivity extends AppCompatActivity {

    static final String EXTRA_ID = "id";
    static final String EXTRA_NOME = "nome";
    static final String EXTRA_TEM_FACE = "temface";

    private PersonInfo pessoa;
    private EnrollmentSession sessao;
    private FaceCapture captura;
    private TextView lbInstrucao;
    private TextView lbConfirma;
    private View painelConfirma;
    private View progresso;
    private View btEnviar;
    private View btOutra;
    private ImageView imgCaptura;

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
        if (Sessao.admin == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_cadastro_face);

        pessoa = new PersonInfo(getIntent().getStringExtra(EXTRA_ID), getIntent().getStringExtra(EXTRA_NOME),
                getIntent().getBooleanExtra(EXTRA_TEM_FACE, false));
        ((TextView) findViewById(R.id.lbPessoa)).setText(pessoa.getName()
                + (pessoa.hasFace() ? "\n(a face atual será substituída)" : ""));

        lbInstrucao = findViewById(R.id.lbInstrucao);
        lbConfirma = findViewById(R.id.lbConfirma);
        painelConfirma = findViewById(R.id.painelConfirma);
        imgCaptura = findViewById(R.id.imgCaptura);
        progresso = findViewById(R.id.progresso);
        btEnviar = findViewById(R.id.btEnviar);
        btOutra = findViewById(R.id.btOutra);

        findViewById(R.id.btGiraCam).setOnClickListener(v -> {
            if (sessao != null) {
                sessao.switchCamera();
            }
        });
        btOutra.setOnClickListener(v -> tirarOutra());
        btEnviar.setOnClickListener(v -> enviar());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciar();
        } else {
            pedirCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciar() {
        PreviewView preview = findViewById(R.id.preview);
        sessao = AIFacial.startEnrollment(this, preview, new EnrollmentListener() {
            @Override
            public void onFeedback(@NonNull EnrollmentFeedback f) {
                lbInstrucao.setText(instrucao(f));
            }

            @Override
            public void onCaptured(@NonNull FaceCapture c) {
                captura = c;
                imgCaptura.setImageBitmap(c.getFace());
                lbInstrucao.setText("Foto capturada");
                lbConfirma.setText("Usar esta foto?");
                painelConfirma.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(@NonNull AIFacialException error) {
                Toast.makeText(CadastroFaceActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void tirarOutra() {
        captura = null;
        painelConfirma.setVisibility(View.GONE);
        sessao.retake();
    }

    private void enviar() {
        if (captura == null) {
            return;
        }
        ocupado(true);
        lbConfirma.setText("Enviando...");
        Sessao.admin.updateFace(pessoa, captura, new AIFacialCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(CadastroFaceActivity.this, "Face de " + pessoa.getName() + " cadastrada",
                        Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(@NonNull AIFacialException error) {
                ocupado(false);
                lbConfirma.setText("Erro: " + error.getMessage());
            }
        });
    }

    private void ocupado(boolean sim) {
        progresso.setVisibility(sim ? View.VISIBLE : View.GONE);
        btEnviar.setEnabled(!sim);
        btOutra.setEnabled(!sim);
    }

    private static String instrucao(EnrollmentFeedback f) {
        switch (f) {
            case NO_FACE:
                return "Posicione o rosto na moldura";
            case MULTIPLE_FACES:
                return "Só uma pessoa na imagem";
            case MOVE_CLOSER:
                return "Aproxime-se";
            case CENTER_FACE:
                return "Centralize o rosto";
            case LOOK_STRAIGHT:
                return "Olhe de frente para a câmera";
            case OPEN_EYES:
                return "Mantenha os olhos abertos";
            case POOR_LIGHT:
                return "Melhore a iluminação";
            default:
                return "Fique parado...";
        }
    }
}
