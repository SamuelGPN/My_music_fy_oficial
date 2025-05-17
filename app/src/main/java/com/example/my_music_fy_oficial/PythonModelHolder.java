package com.example.my_music_fy_oficial;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

//  criei essa classe para que o python possa ser inicializado mais rápido no music activity

public class PythonModelHolder {
    public static PyObject model; // corresponde a main.py
    //public static PyObject model2; // corresponde a main_processing.py

    public static void initModels() {
        if (!Python.isStarted()) {
            throw new IllegalStateException("Python não foi iniciado.");
        }
        Python py = Python.getInstance();
        model = py.getModule("main");// chama o primeiro modulo do python para esse activity
        //model2 = py.getModule("main_processing");//inicializa a classe python e chama outro modulo "main_processing" para o MusicActivity
    }

    // Método para chamar modelo1 passando parâmetros
    public static PyObject callModeloFromModel1(Object... params) {
        if (model == null) {
            throw new IllegalStateException("Model não foi inicializado.");
        }
        return model.callAttr("pesquisar", params);
    }

    // Método para chamar modelo2 passando parâmetros
    public static PyObject callModeloFromModel2(Object... params) {
        if (model == null) {
            throw new IllegalStateException("Model não foi inicializado.");
        }
        return model.callAttr("baixar_musica_temp", params);
    }

}
