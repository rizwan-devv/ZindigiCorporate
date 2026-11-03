package com.example.kycapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kycapp.dfs.DfsApiException
import com.example.kycapp.dfs.DfsSession
import com.example.kycapp.portal.EntityTypeOption
import com.example.kycapp.portal.PartySnapshot
import com.example.kycapp.portal.PortalApiClient
import com.example.kycapp.portal.PortalSession
import com.example.kycapp.ui.components.AppTextField
import com.example.kycapp.ui.components.DfsBrandMark
import com.example.kycapp.ui.components.DfsCard
import com.example.kycapp.ui.components.DfsPrimaryButton
import com.example.kycapp.ui.components.DfsScreen
import com.example.kycapp.ui.components.DfsSecondaryButton
import com.example.kycapp.ui.components.DfsStatusBadge
import com.example.kycapp.ui.theme.DfsColors
import kotlinx.coroutines.launch
import org.json.JSONObject

private fun needsPartners(entityType: String) =
    entityType == "PARTNERSHIP" || entityType == "LLP"

@Composable
fun OnboardingScreen(
    portal: PortalSession,
    onPersonalKycReady: (DfsSession) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var party by remember { mutableStateOf<PartySnapshot?>(null) }
    var entityTypes by remember { mutableStateOf<List<EntityTypeOption>>(emptyList()) }
    var step by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var entityType by remember { mutableStateOf("SOLE_PROPRIETORSHIP") }
    var partnershipUnregistered by remember { mutableStateOf(false) }
    var applicantIsPartner by remember { mutableStateOf(true) }
    var incorporationNumber by remember { mutableStateOf("") }
    var incorporationDate by remember { mutableStateOf("") }
    var incorporationCountry by remember { mutableStateOf("Pakistan") }
    var incorporationAuthority by remember { mutableStateOf("SECP") }
    var ntnNumber by remember { mutableStateOf("") }
    var registeredAddress by remember { mutableStateOf("") }
    var mailingAddress by remember { mutableStateOf("") }
    var placeOfBusiness by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var natureOfBusiness by remember { mutableStateOf("") }
    var purposeOfAccount by remember { mutableStateOf("") }
    var intendedRelationship by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    var partnerName by remember { mutableStateOf("") }
    var partnerPhone by remember { mutableStateOf("") }
    var partnerEmail by remember { mutableStateOf("") }

    var pendingDocCode by remember { mutableStateOf<String?>(null) }
    var entityMenuOpen by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val code = pendingDocCode
        pendingDocCode = null
        if (uri == null || code == null) return@rememberLauncherForActivityResult
        loading = true
        error = null
        scope.launch {
            try {
                party = PortalApiClient.uploadDocument(context, portal.token, code, uri)
                ok = "$code uploaded"
            } catch (e: Exception) {
                error = (e as? DfsApiException)?.message ?: (e.message ?: "Upload failed")
            } finally {
                loading = false
            }
        }
    }

    fun applyParty(p: PartySnapshot) {
        party = p
        step = when {
            p.status != "DRAFT" && p.status != "REJECTED" -> 4
            needsPartners(p.entityType ?: entityType) -> p.onboardingStep.coerceIn(1, 4)
            else -> when (p.onboardingStep) {
                1 -> 1
                2 -> 3
                else -> p.onboardingStep.coerceIn(1, 4)
            }
        }
        fullName = p.fullName ?: fullName
        businessName = p.businessName ?: businessName
        entityType = p.entityType ?: entityType
        partnershipUnregistered = p.partnershipUnregistered
        applicantIsPartner = if (!needsPartners(entityType)) true else p.applicantIsPartner
        phone = p.phone ?: phone
    }

    LaunchedEffect(portal.token) {
        try {
            entityTypes = PortalApiClient.entityTypes()
            applyParty(PortalApiClient.me(portal.token))
        } catch (e: Exception) {
            error = (e as? DfsApiException)?.message ?: (e.message ?: "Failed to load")
        }
    }

    fun saveEntity() {
        error = null
        ok = null
        loading = true
        scope.launch {
            try {
                val owner = !needsPartners(entityType)
                val body = JSONObject()
                    .put("fullName", fullName.trim())
                    .put("businessName", businessName.trim())
                    .put("entityType", entityType)
                    .put("partnershipUnregistered", partnershipUnregistered)
                    .put("applicantIsPartner", if (owner) true else applicantIsPartner)
                    .put("incorporationNumber", incorporationNumber.trim())
                    .put("incorporationCountry", incorporationCountry.trim())
                    .put("incorporationAuthority", incorporationAuthority.trim())
                    .put("ntnNumber", ntnNumber.trim())
                    .put("taxCountry", "Pakistan")
                    .put("registeredAddress", registeredAddress.trim())
                    .put("mailingAddress", mailingAddress.trim())
                    .put("placeOfBusiness", placeOfBusiness.trim())
                    .put("city", city.trim())
                    .put("country", "Pakistan")
                    .put("phone", phone.trim())
                    .put("natureOfBusiness", natureOfBusiness.trim())
                    .put("purposeOfAccount", purposeOfAccount.trim())
                    .put("intendedRelationship", intendedRelationship.trim())
                    .put("termsAccepted", termsAccepted)
                    .put("onboardingStep", if (owner) 3 else 2)
                if (incorporationDate.isNotBlank()) {
                    body.put("incorporationDate", incorporationDate.trim())
                }
                val p = PortalApiClient.saveProfile(portal.token, body)
                applyParty(p)
                ok = "Entity saved"
                step = if (owner) 3 else 2
            } catch (e: Exception) {
                error = (e as? DfsApiException)?.message ?: (e.message ?: "Save failed")
            } finally {
                loading = false
            }
        }
    }

    fun addPartner() {
        error = null
        loading = true
        scope.launch {
            try {
                applyParty(PortalApiClient.addPartner(portal.token, partnerName, partnerPhone, partnerEmail))
                partnerName = ""
                partnerPhone = ""
                partnerEmail = ""
                ok = "Partner added"
            } catch (e: Exception) {
                error = (e as? DfsApiException)?.message ?: (e.message ?: "Failed")
            } finally {
                loading = false
            }
        }
    }

    fun submitAndStartKyc() {
        error = null
        ok = null
        loading = true
        scope.launch {
            try {
                val p = PortalApiClient.submit(portal.token)
                applyParty(p)
                ok = "Submitted ${p.trackingId}. Starting personal KYC…"
                val kyc = PortalApiClient.startPersonalKyc(portal.token)
                onPersonalKycReady(kyc)
            } catch (e: Exception) {
                error = (e as? DfsApiException)?.message ?: (e.message ?: "Submit failed")
                // If already submitted, try personal KYC anyway
                try {
                    val kyc = PortalApiClient.startPersonalKyc(portal.token)
                    onPersonalKycReady(kyc)
                } catch (_: Exception) {
                }
            } finally {
                loading = false
            }
        }
    }

    fun continuePersonalKyc() {
        loading = true
        error = null
        scope.launch {
            try {
                onPersonalKycReady(PortalApiClient.startPersonalKyc(portal.token))
            } catch (e: Exception) {
                error = (e as? DfsApiException)?.message ?: (e.message ?: "Could not start KYC")
            } finally {
                loading = false
            }
        }
    }

    val editable = party?.status == "DRAFT" || party?.status == "REJECTED"
    val partnerMode = needsPartners(entityType)
    val label = entityTypes.firstOrNull { it.code == entityType }?.label ?: entityType

    DfsScreen {
        DfsBrandMark(compact = true)
        Spacer(modifier = Modifier.height(12.dp))
        DfsStatusBadge(label = "Onboarding")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Entity KYC",
            color = DfsColors.OnBackground,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tracking: ${party?.trackingId ?: portal.trackingId ?: "—"} · ${party?.status ?: portal.partyStatus}",
            color = DfsColors.MutedText,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Entity", if (partnerMode) "Partners" else null, "Docs", "Review")
                .filterNotNull()
                .forEachIndexed { i, t ->
                    val active = when {
                        !partnerMode && i == 0 -> step == 1
                        !partnerMode && i == 1 -> step == 3
                        !partnerMode && i == 2 -> step >= 4
                        else -> step == i + 1
                    }
                    Text(
                        text = t,
                        color = if (active) DfsColors.Primary else DfsColors.MutedText,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
        }

        Spacer(modifier = Modifier.height(14.dp))
        if (error != null) {
            DfsCard { Text(error!!, color = DfsColors.Danger, fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (ok != null) {
            DfsCard { Text(ok!!, color = DfsColors.Success, fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(10.dp))
        }
        party?.rejectionReason?.let {
            DfsCard { Text("Rejected: $it", color = DfsColors.Danger, fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (editable && step == 1) {
            DfsCard {
                FieldLabel("Business / legal name")
                AppTextField(businessName, { businessName = it }, "Business name")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Contact name")
                AppTextField(fullName, { fullName = it }, "Full name")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Entity type")
                Column {
                    Text(
                        text = label,
                        color = DfsColors.OnBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { entityMenuOpen = true }
                            .padding(vertical = 10.dp),
                        fontSize = 14.sp
                    )
                    DropdownMenu(expanded = entityMenuOpen, onDismissRequest = { entityMenuOpen = false }) {
                        (entityTypes.ifEmpty {
                            listOf(
                                EntityTypeOption("SOLE_PROPRIETORSHIP", "Sole Proprietorship"),
                                EntityTypeOption("SMALL_BUSINESS", "Small business"),
                                EntityTypeOption("PARTNERSHIP", "Partnership"),
                                EntityTypeOption("LLP", "LLP")
                            )
                        }).forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    entityType = opt.code
                                    if (!needsPartners(opt.code)) applicantIsPartner = true
                                    entityMenuOpen = false
                                }
                            )
                        }
                    }
                }
                if (entityType == "PARTNERSHIP") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = partnershipUnregistered,
                            onCheckedChange = { partnershipUnregistered = it },
                            colors = CheckboxDefaults.colors(checkedColor = DfsColors.Primary)
                        )
                        Text("Unregistered partnership", color = DfsColors.MutedText, fontSize = 13.sp)
                    }
                }
                if (partnerMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = applicantIsPartner,
                            onCheckedChange = { applicantIsPartner = it },
                            colors = CheckboxDefaults.colors(checkedColor = DfsColors.Primary)
                        )
                        Text("I am also a partner", color = DfsColors.MutedText, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                FieldLabel("Incorporation number")
                AppTextField(incorporationNumber, { incorporationNumber = it }, "Optional")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Incorporation date (YYYY-MM-DD)")
                AppTextField(incorporationDate, { incorporationDate = it }, "2024-01-15")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Country / authority")
                AppTextField(incorporationCountry, { incorporationCountry = it }, "Pakistan")
                Spacer(modifier = Modifier.height(10.dp))
                AppTextField(incorporationAuthority, { incorporationAuthority = it }, "SECP")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("NTN")
                AppTextField(ntnNumber, { ntnNumber = it }, "Optional")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Registered address")
                AppTextField(registeredAddress, { registeredAddress = it }, "Address")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Mailing / place of business")
                AppTextField(mailingAddress, { mailingAddress = it }, "Mailing")
                Spacer(modifier = Modifier.height(10.dp))
                AppTextField(placeOfBusiness, { placeOfBusiness = it }, "Place of business")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("City")
                AppTextField(city, { city = it }, "City")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Phone (app user ID)")
                AppTextField(phone, { phone = it }, "03XXXXXXXXX", KeyboardType.Phone)
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Nature of business")
                AppTextField(natureOfBusiness, { natureOfBusiness = it }, "Describe", singleLine = false)
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Purpose of account")
                AppTextField(purposeOfAccount, { purposeOfAccount = it }, "Purpose")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Intended relationship")
                AppTextField(intendedRelationship, { intendedRelationship = it }, "Optional")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = DfsColors.Primary)
                    )
                    Text("I accept terms & conditions", color = DfsColors.OnBackground, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                DfsPrimaryButton(
                    text = if (loading) "Saving…" else "Save & continue",
                    onClick = { saveEntity() },
                    enabled = !loading && termsAccepted && businessName.isNotBlank()
                        && fullName.isNotBlank() && registeredAddress.isNotBlank()
                        && natureOfBusiness.isNotBlank() && purposeOfAccount.isNotBlank()
                )
            }
        }

        if (editable && partnerMode && step == 2) {
            DfsCard {
                Text("Partner roster", color = DfsColors.OnBackground, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                party?.associatedPersons?.forEach { p ->
                    Text(
                        "${p.fullName} · ${p.phone ?: "—"} · ${p.email ?: "—"}",
                        color = DfsColors.MutedText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                FieldLabel("Partner name")
                AppTextField(partnerName, { partnerName = it }, "Full name")
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Phone")
                AppTextField(partnerPhone, { partnerPhone = it }, "03XXXXXXXXX", KeyboardType.Phone)
                Spacer(modifier = Modifier.height(10.dp))
                FieldLabel("Email")
                AppTextField(partnerEmail, { partnerEmail = it }, "partner@email.com", KeyboardType.Email)
                Spacer(modifier = Modifier.height(14.dp))
                DfsPrimaryButton(
                    text = if (loading) "Adding…" else "Add partner",
                    onClick = { addPartner() },
                    enabled = !loading && partnerName.isNotBlank() && partnerPhone.isNotBlank()
                        && partnerEmail.isNotBlank()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DfsSecondaryButton(text = "Continue to documents", onClick = { step = 3 })
            }
        }

        if (editable && step == 3) {
            DfsCard {
                Text("Annex-C documents", color = DfsColors.OnBackground, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Upload required files. Submit unlocks when mandatory docs are present.",
                    color = DfsColors.MutedText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                party?.requiredDocuments?.forEach { doc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.documentLabel, color = DfsColors.OnBackground, fontSize = 13.sp)
                            Text(
                                "${doc.documentCode} · ${if (doc.mandatory) "required" else "optional"}" +
                                    if (doc.uploaded) " · uploaded" else "",
                                color = if (doc.uploaded) DfsColors.Success else DfsColors.MutedText,
                                fontSize = 11.sp
                            )
                        }
                        DfsSecondaryButton(
                            text = if (doc.uploaded) "Replace" else "Upload",
                            onClick = {
                                pendingDocCode = doc.documentCode
                                picker.launch("*/*")
                            },
                            enabled = !loading
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                DfsPrimaryButton(
                    text = "Review & submit",
                    onClick = { step = 4 },
                    enabled = !loading
                )
                if (partnerMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DfsSecondaryButton(text = "Back to partners", onClick = { step = 2 })
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    DfsSecondaryButton(text = "Back to entity", onClick = { step = 1 })
                }
            }
        }

        if (step >= 4) {
            DfsCard {
                Text("Review", color = DfsColors.OnBackground, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${party?.businessName ?: businessName}\n${party?.entityType ?: entityType}\nStatus: ${party?.status}",
                    color = DfsColors.MutedText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                val missing = party?.requiredDocuments?.filter { it.mandatory && !it.uploaded }.orEmpty()
                if (missing.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Still needed: ${missing.joinToString { it.documentCode }}",
                        color = DfsColors.Warning,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (editable) {
                    DfsPrimaryButton(
                        text = if (loading) "Submitting…" else "Submit & start personal KYC",
                        onClick = { submitAndStartKyc() },
                        enabled = !loading && (party?.canSubmit == true)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DfsSecondaryButton(text = "Back to documents", onClick = { step = 3 })
                } else {
                    DfsPrimaryButton(
                        text = if (loading) "Opening…" else "Continue personal KYC",
                        onClick = { continuePersonalKyc() },
                        enabled = !loading
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        DfsSecondaryButton(text = "Sign out", onClick = onSignOut)
    }
}
