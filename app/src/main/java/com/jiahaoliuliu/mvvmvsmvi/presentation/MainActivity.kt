package com.jiahaoliuliu.mvvmvsmvi.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jiahaoliuliu.mvvmvsmvi.R
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvi.MviActivity
import com.jiahaoliuliu.mvvmvsmvi.presentation.mvvm.MvvmActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.choose_architecture),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(stringResource(R.string.intro))
                        Text(stringResource(R.string.mvvm_explanation))
                        Text(stringResource(R.string.mvi_explanation))
                        Text(
                            stringResource(R.string.shared_explanation),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        MvvmActivity::class.java
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.open_mvvm))
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        MviActivity::class.java
                                    )
                                )
                            }) {
                            Text(stringResource(R.string.open_mvi))
                        }
                    }
                }
            }
        }
    }
}
