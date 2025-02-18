package com.example.beaconble.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.beaconble.AppMain
import com.example.beaconble.BuildConfig
import com.example.beaconble.databinding.FragmentManageSessionsBinding
import com.example.beaconble.databinding.RowItemSessionFileBinding
import java.io.File

class FragManageSessions : Fragment() {

    private lateinit var binding: FragmentManageSessionsBinding
    private val app = AppMain.instance
    private lateinit var sessionFilesAdapter: SessionFilesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentManageSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionFilesAdapter = SessionFilesAdapter(app.loggingSession.getSessionFiles())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sessionFilesAdapter
        }

        binding.btnDelete.setOnClickListener {
            val selectedFiles = sessionFilesAdapter.getSelectedFiles()
            selectedFiles.forEach { it.delete() }
            sessionFilesAdapter.updateFiles(app.loggingSession.getSessionFiles())
            Toast.makeText(context, "Selected files deleted", Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            val selectedFiles = sessionFilesAdapter.getSelectedFiles()
            if (selectedFiles.isNotEmpty()) {
                shareFiles(selectedFiles)
            } else {
                Toast.makeText(context, "No files selected to share", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareFiles(files: List<File>) {
        val uris = files.map {
            FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".fileProvider", it)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share session files"))
    }

    class SessionFilesAdapter(private var files: List<File>) :
        RecyclerView.Adapter<SessionFilesAdapter.ViewHolder>() {

        private val selectedFiles = mutableSetOf<File>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = RowItemSessionFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            holder.bind(file)
        }

        override fun getItemCount(): Int = files.size

        fun getSelectedFiles(): List<File> = selectedFiles.toList()

        fun updateFiles(newFiles: List<File>) {
            files = newFiles
            selectedFiles.clear()
            notifyDataSetChanged()
        }

        inner class ViewHolder(private val binding: RowItemSessionFileBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(file: File) {
                binding.fileName.text = file.name

                binding.btnShare.setOnClickListener {
                    val uri = FileProvider.getUriForFile(binding.root.context, BuildConfig.APPLICATION_ID + ".fileProvider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    binding.root.context.startActivity(Intent.createChooser(intent, "Share session file"))
                }
                binding.btnDelete.setOnClickListener {
                    file.delete()
                    updateFiles(files - file)
                    Toast.makeText(binding.root.context, "File deleted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
