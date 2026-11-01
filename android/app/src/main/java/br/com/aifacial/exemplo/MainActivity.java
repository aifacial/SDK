package br.com.aifacial.exemplo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import br.com.aifacial.sdk.AIFacial;
import br.com.aifacial.sdk.AIFacialAdmin;
import br.com.aifacial.sdk.AIFacialCallback;
import br.com.aifacial.sdk.AIFacialConfig;
import br.com.aifacial.sdk.AIFacialException;
import br.com.aifacial.sdk.LivenessMode;
import br.com.aifacial.sdk.PrepareCallback;
import br.com.aifacial.sdk.SyncResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela inicial do app de exemplo: configura o SDK, prepara (baixa a IA e sincroniza)
 * e abre o reconhecimento. Mostra o mínimo que um app cliente precisa fazer.
 */
public class MainActivity extends AppCompatActivity {

    private EditText edEmpresa, edToken, edTokenEmpresa;
    private CheckBox ckLiveness, ckAuditoria;
    private ProgressBar progresso;
    private TextView lbStatus;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("exemplo", MODE_PRIVATE);
        edEmpresa = findViewById(R.id.edEmpresa);
        edToken = findViewById(R.id.edToken);
        edTokenEmpresa = findViewById(R.id.edTokenEmpresa);
        progresso = findViewById(R.id.progresso);
        lbStatus = findViewById(R.id.lbStatus);

        ckLiveness = findViewById(R.id.ckLiveness);
        ckAuditoria = findViewById(R.id.ckAuditoria);

        // Pré-preenche com o local.properties (veja o README) na primeira vez.
        edEmpresa.setText(prefs.getString("empresa", BuildConfig.COMPANY_ID));
        edToken.setText(prefs.getString("token", BuildConfig.MOBILE_TOKEN));
        edTokenEmpresa.setText(prefs.getString("tokenEmpresa", BuildConfig.COMPANY_TOKEN));
        ckLiveness.setChecked(prefs.getBoolean("liveness", false));
        ckAuditoria.setChecked(prefs.getBoolean("auditoria", false));
        ((TextView) findViewById(R.id.lbVersao)).setText("SDK AIFacial " + AIFacial.VERSION);

        findViewById(R.id.btPreparar).setOnClickListener(v -> {
            if (configurar()) {
                AIFacial.prepare(callback);
            }
        });
        findViewById(R.id.btSincronizar).setOnClickListener(v -> {
            if (configurar()) {
                AIFacial.syncPersons(callback);
            }
        });
        findViewById(R.id.btBaixarIA).setOnClickListener(v -> {
            if (configurar()) {
                // Baixa (ou atualiza) só o modelo de IA; as pessoas do aparelho não mudam.
                AIFacial.updateModel(callback);
            }
        });
        findViewById(R.id.btSincLista).setOnClickListener(v -> {
            if (!configurar()) {
                return;
            }
            List<String> ids = new ArrayList<>();
            for (String id : ((EditText) findViewById(R.id.edIds)).getText().toString().split("[,;\\s]+")) {
                if (!id.trim().isEmpty()) {
                    ids.add(id.trim());
                }
            }
            if (ids.isEmpty()) {
                Toast.makeText(this, "Informe os IDs das pessoas", Toast.LENGTH_SHORT).show();
                return;
            }
            ocupado(true);
            progresso.setIndeterminate(true);
            lbStatus.setText("Sincronizando " + ids.size() + " pessoa(s)...");
            AIFacial.syncPersons(ids, new AIFacialCallback<SyncResult>() {
                @Override
                public void onSuccess(SyncResult r) {
                    ocupado(false);
                    mostrarSituacao();
                    String msg = r.getReceived() + " pessoa(s) baixada(s)";
                    if (!r.getNotFound().isEmpty()) {
                        msg += "\nSem face ou inexistentes: " + String.join(", ", r.getNotFound());
                    }
                    if (!r.getDeleted().isEmpty()) {
                        msg += "\nApagadas do aparelho (excluídas): " + String.join(", ", r.getDeleted());
                    }
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(@NonNull AIFacialException error) {
                    ocupado(false);
                    lbStatus.setText("Erro (" + error.getCode() + "): " + error.getMessage());
                }
            });
        });
        findViewById(R.id.btApagarPessoas).setOnClickListener(v -> {
            if (configurar()) {
                AIFacial.clearPersons();
                lbStatus.setText("Pessoas apagadas do aparelho (IA mantida)");
                lbStatus.postDelayed(this::mostrarSituacao, 500);
            }
        });
        findViewById(R.id.btReconhecer).setOnClickListener(v -> {
            if (!configurar()) {
                return;
            }
            if (!AIFacial.isModelReady()) {
                Toast.makeText(this, "Toque em Preparar primeiro", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, ReconhecimentoActivity.class));
        });
        findViewById(R.id.btCadastro).setOnClickListener(v -> {
            if (!configurar()) {
                return;
            }
            if (!AIFacial.isModelReady()) {
                Toast.makeText(this, "Toque em Preparar primeiro", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Sessao.admin != null) {
                startActivity(new Intent(this, PessoasActivity.class));
            } else {
                pedirLogin();
            }
        });
        findViewById(R.id.btLimpar).setOnClickListener(v -> {
            if (configurar()) {
                AIFacial.clearData();
                lbStatus.setText("Dados apagados");
            }
        });

        if (!edEmpresa.getText().toString().isEmpty() && configurar()) {
            mostrarSituacao();
        }
    }

    /** Lê os campos e chama AIFacial.init(). Num app real isso fica no Application.onCreate(). */
    private boolean configurar() {
        String empresa = edEmpresa.getText().toString().trim();
        String token = edToken.getText().toString().trim();
        String tokenEmpresa = edTokenEmpresa.getText().toString().trim();
        if (empresa.isEmpty() || token.isEmpty() || tokenEmpresa.isEmpty()) {
            Toast.makeText(this, "Informe a empresa e os dois tokens", Toast.LENGTH_SHORT).show();
            return false;
        }
        prefs.edit().putString("empresa", empresa).putString("token", token)
                .putString("tokenEmpresa", tokenEmpresa)
                .putBoolean("liveness", ckLiveness.isChecked())
                .putBoolean("auditoria", ckAuditoria.isChecked()).apply();
        AIFacial.init(this, new AIFacialConfig.Builder(empresa, token, tokenEmpresa)
                .liveness(ckLiveness.isChecked() ? LivenessMode.ACTIVE : LivenessMode.NONE)
                .livenessChallenges(1)
                .audit(ckAuditoria.isChecked())
                .build());
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AIFacial.isInitialized()) {
            mostrarSituacao();
        }
    }

    private void mostrarSituacao() {
        ((TextView) findViewById(R.id.lbAuditoria)).setText(
                "Auditoria pendente de envio: " + AIFacial.getPendingAuditCount());
        lbStatus.setText((AIFacial.isModelReady() ? "IA pronta" : "IA não baixada")
                + " · " + AIFacial.getPersonCount() + " pessoas no aparelho");
    }

    /** Login do painel do cliente (a api/v1 exige). Num app real, controle quem pode acessar. */
    private void pedirLogin() {
        LinearLayout campos = new LinearLayout(this);
        campos.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        campos.setPadding(pad, pad / 2, pad, 0);
        EditText usuario = new EditText(this);
        usuario.setHint("Usuário");
        usuario.setInputType(InputType.TYPE_CLASS_TEXT);
        EditText senha = new EditText(this);
        senha.setHint("Senha");
        senha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Preenche com o login do local.properties ou com o último usuário usado.
        usuario.setText(BuildConfig.LOGIN_USER.isEmpty()
                ? prefs.getString("loginUsuario", "") : BuildConfig.LOGIN_USER);
        senha.setText(BuildConfig.LOGIN_PASSWORD);
        campos.addView(usuario);
        campos.addView(senha);

        new AlertDialog.Builder(this)
                .setTitle("Login do painel")
                .setView(campos)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Entrar", (d, w) -> {
                    lbStatus.setText("Entrando...");
                    AIFacialAdmin.login(usuario.getText().toString(), senha.getText().toString(),
                            new AIFacialCallback<AIFacialAdmin>() {
                                @Override
                                public void onSuccess(AIFacialAdmin admin) {
                                    Sessao.admin = admin;
                                    prefs.edit().putString("loginUsuario", usuario.getText().toString()).apply();
                                    mostrarSituacao();
                                    startActivity(new Intent(MainActivity.this, PessoasActivity.class));
                                }

                                @Override
                                public void onError(@NonNull AIFacialException error) {
                                    lbStatus.setText("Login: " + error.getMessage());
                                }
                            });
                })
                .show();
    }

    private void ocupado(boolean sim) {
        progresso.setVisibility(sim ? View.VISIBLE : View.INVISIBLE);
        for (int id : new int[]{R.id.btPreparar, R.id.btBaixarIA, R.id.btSincronizar, R.id.btSincLista,
                R.id.btApagarPessoas,
                R.id.btReconhecer, R.id.btCadastro, R.id.btLimpar}) {
            ((Button) findViewById(id)).setEnabled(!sim);
        }
    }

    private final PrepareCallback callback = new PrepareCallback() {
        @Override
        public void onProgress(@NonNull Step step, int percent) {
            ocupado(true);
            progresso.setIndeterminate(percent < 0);
            progresso.setProgress(Math.max(0, percent));
            lbStatus.setText(step == Step.DOWNLOADING_MODEL
                    ? "Baixando IA: " + percent + "%"
                    : "Sincronizando pessoas: " + percent + "%");
        }

        @Override
        public void onReady(int personCount) {
            ocupado(false);
            mostrarSituacao();
        }

        @Override
        public void onError(@NonNull AIFacialException error) {
            ocupado(false);
            lbStatus.setText("Erro (" + error.getCode() + "): " + error.getMessage());
        }
    };
}
