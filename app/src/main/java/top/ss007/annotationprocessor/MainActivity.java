package top.ss007.annotationprocessor;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import top.ss007.annotation.BindView;
import top.ss007.annotation.OnClick;
import top.ss007.binder.Binding;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_hello)
    TextView tvHello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Binding.bind(this);
    }

    @OnClick(R.id.btn_click)
    void onHelloBtnClick(View v){
        tvHello.setText("hello android annotation processor");
    }
}
