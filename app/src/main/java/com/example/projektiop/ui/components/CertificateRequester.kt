package com.example.projektiop.ui.components

/*
@Composable
fun CertificateRequester(
    authToken: String,
) {
    val certificateUtils = koinInject<CertificateUtils>()
    var result by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val certificateApi = koinInject<CertificateApi>()
    val userRepository = koinInject<UserRepository>()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Request Certificate", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    result = null
                    try {
                        val profileResult = try {
                            userRepository.fetchMyProfile()
                        } catch (e: Exception) {
                            Result.failure(e)
                        }

                        profileResult.fold(
                            onSuccess = { profileData ->
                                val userEmail: String = profileData.email.toString()
                                println("User email: $userEmail")
                                try {
                                    val keyPair = certificateUtils.generateKeyPair()
                                    var csrPem = certificateUtils.generateCSR(userEmail, keyPair)
                                    println("CSR generated: $csrPem")
                                    val certificateResponse = try {
                                        certificateApi.issueCertificate(
                                            token = "Bearer $authToken",
                                            request = CertificateRequest(csrPem)
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }

                                    result = if (certificateResponse != null) {
                                        certificateResponse.certPem
                                    } else {
                                        "Error: Failed to issue certificate"
                                    }
                                } catch (e: Exception) {
                                    println(e)
                                }
                            },
                            onFailure = { exception ->
                                println("Error fetching user profile: ${exception.message}")
                            })


                    } catch (e: Exception) {
                        error = "Failed: ${e.localizedMessage}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading
        ) {
            Text(if (loading) "Requesting..." else "Request Certificate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            result != null -> Text(result!!, style = MaterialTheme.typography.bodySmall)
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
*/