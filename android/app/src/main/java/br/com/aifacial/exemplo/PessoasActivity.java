package br.com.aifacial.exemplo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.aifacial.sdk.AIFacial;
import br.com.aifacial.sdk.AIFacialCallback;
import br.com.aifacial.sdk.AIFacialException;
import br.com.aifacial.sdk.PersonInfo;

/**
 * Lista as pessoas sincronizadas NESTE APARELHO (AIFacial.getLocalPersons, sem internet)
 * e abre o cadastro de face da pessoa escolhida. O envio usa o login do painel (Sessao.admin).
 */
public class PessoasActivity extends AppCompatActivity {

    private final List<PersonInfo> todas = new ArrayList<>();
    private final List<PersonInfo> visiveis = new ArrayList<>();
    private final Adaptador adaptador = new Adaptador();
    private EditText edBusca;
    private TextView lbTotal;

    private final ActivityResultLauncher<Intent> cadastrar =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) {
                    carregar();   // atualiza a foto da pessoa cadastrada
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Sessao.admin == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_pessoas);
        setTitle("Pessoas neste aparelho");

        edBusca = findViewById(R.id.edBusca);
        lbTotal = findViewById(R.id.lbTotal);
        findViewById(R.id.btMais).setVisibility(View.GONE);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        // Filtra enquanto digita (nome ou ID, sem diferenciar acentos/maiúsculas).
        edBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                filtrar();
            }
        });
        carregar();
    }

    private void carregar() {
        AIFacial.getLocalPersons(new AIFacialCallback<List<PersonInfo>>() {
            @Override
            public void onSuccess(List<PersonInfo> pessoas) {
                todas.clear();
                todas.addAll(pessoas);
                filtrar();
            }

            @Override
            public void onError(@NonNull AIFacialException error) {
                Toast.makeText(PessoasActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filtrar() {
        String busca = normalizar(edBusca.getText().toString());
        visiveis.clear();
        for (PersonInfo p : todas) {
            if (busca.isEmpty() || normalizar(p.getName()).contains(busca) || p.getId().contains(busca)) {
                visiveis.add(p);
            }
        }
        adaptador.notifyDataSetChanged();
        lbTotal.setText(todas.isEmpty()
                ? "Nenhuma pessoa neste aparelho. Sincronize antes."
                : visiveis.size() + " de " + todas.size() + " pessoa(s) neste aparelho");
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private void abrir(PersonInfo p) {
        Intent i = new Intent(this, CadastroFaceActivity.class);
        i.putExtra(CadastroFaceActivity.EXTRA_ID, p.getId());
        i.putExtra(CadastroFaceActivity.EXTRA_NOME, p.getName());
        i.putExtra(CadastroFaceActivity.EXTRA_TEM_FACE, p.hasFace());
        cadastrar.launch(i);
    }

    private final class Adaptador extends RecyclerView.Adapter<Adaptador.Linha> {

        final class Linha extends RecyclerView.ViewHolder {
            final ImageView foto;
            final TextView nome;
            final TextView id;

            Linha(View v) {
                super(v);
                foto = v.findViewById(R.id.imgFoto);
                nome = v.findViewById(R.id.txtNome);
                id = v.findViewById(R.id.txtId);
            }
        }

        @NonNull
        @Override
        public Linha onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Linha(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pessoa, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Linha h, int position) {
            PersonInfo p = visiveis.get(position);
            h.nome.setText(p.getName());
            h.id.setText("ID " + p.getId());
            h.foto.setImageBitmap(p.getPhoto());
            h.itemView.setOnClickListener(v -> abrir(p));
        }

        @Override
        public int getItemCount() {
            return visiveis.size();
        }
    }
}
