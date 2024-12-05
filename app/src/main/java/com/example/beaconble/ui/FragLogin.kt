package com.example.beaconble.ui

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.beaconble.R

class FragLogin : Fragment() {
    lateinit var editTextUsername : EditText
    lateinit var editTextPassword : EditText
    lateinit var buttonLogin: Button

    companion object {
        fun newInstance() = FragLogin()
    }

    private val viewModel: FragLoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        editTextUsername = view.findViewById<EditText>(R.id.etUsername)
        editTextPassword = view.findViewById<EditText>(R.id.etPassword)
        buttonLogin = view.findViewById<Button>(R.id.btnLogin)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextUsername.setText(viewModel.username)
        editTextPassword.setText(viewModel.password)

        buttonLogin.setOnClickListener {
            viewModel.username = editTextUsername.text.toString()
            viewModel.password = editTextPassword.text.toString()
        }
    }
}