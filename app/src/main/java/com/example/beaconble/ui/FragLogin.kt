package com.example.beaconble.ui

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.text.TextWatcher
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.beaconble.ApiUserSessionState
import com.example.beaconble.R

class FragLogin : Fragment() {
    lateinit var editTextEmail : EditText
    lateinit var editTextPassword : EditText
    lateinit var buttonLogin: Button
    lateinit var progressBar: ProgressBar

    companion object {
        fun newInstance() = FragLogin()
    }

    private val viewModel: FragLoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        editTextEmail = view.findViewById<EditText>(R.id.etEmail)
        editTextPassword = view.findViewById<EditText>(R.id.etPassword)
        buttonLogin = view.findViewById<Button>(R.id.btnLogin)
        progressBar = view.findViewById<ProgressBar>(R.id.pbLogin)

        // observe the login status to show the user any errors or return to the main activity
        viewModel.registerStatus.observe(viewLifecycleOwner) { status ->
            Log.d("FragLogin", "Login status: $status")
            if (status == ApiUserSessionState.LOGGED_IN) {
                // navigate to the main activity
                activity?.supportFragmentManager?.popBackStack()
            } else {
                // show the user the error message
                if (status == ApiUserSessionState.ERROR_BAD_IDENTITY) {
                    editTextEmail.error = getString(R.string.bad_email)
                } else if (status == ApiUserSessionState.ERROR_BAD_PASSWORD) {
                    editTextPassword.error = getString(R.string.bad_password)
                } else if (status == ApiUserSessionState.CONNECTION_ERROR) {
                    // Create an informative alert dialog
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle(R.string.connection_error)
                    builder.setMessage(R.string.connection_error_message)
                    builder.setPositiveButton(R.string.ok) { dialog, which ->
                        // do nothing
                    }
                    builder.show()
                }
            }
            progressBar.visibility = View.INVISIBLE
        }

        // observe the login button enabled status
        viewModel.loginButtonEnabled.observe(viewLifecycleOwner) { enabled ->
            buttonLogin.isEnabled = enabled
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextEmail.setText(viewModel.email.value, TextView.BufferType.EDITABLE)
        editTextPassword.setText(viewModel.password.value, TextView.BufferType.EDITABLE)

        editTextEmail.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.email.value = s.toString()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            }
        )
        editTextPassword.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.password.value = s.toString()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            }
        )

        buttonLogin.setOnClickListener {
            // close the keyboard
            editTextEmail.clearFocus()
            editTextPassword.clearFocus()

            // clear errors on editTexts
            editTextEmail.error = null
            editTextPassword.error = null

            // show the user the login is in progress
            buttonLogin.isEnabled = false
            progressBar.visibility = View.VISIBLE

            viewModel.email.value = editTextEmail.text.toString()
            viewModel.password.value = editTextPassword.text.toString()
            viewModel.doLogin()
        }
    }
}
