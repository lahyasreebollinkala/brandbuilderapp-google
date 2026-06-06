package com.example.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BrandCampaign
import kotlinx.coroutines.launch

// Color Palette for Brand Builder Luxury Theme
private val CharcoalDark = Color(0xFF101012)
private val CardBackground = Color(0xFF1A1A1E)
private val BorderGold = Color(0xFFD4AF37)
private val LightGold = Color(0xFFF3E5AB)
private val SlateText = Color(0xFF9E9E9E)
private val AccentGoldDark = Color(0xFFAA7C11)

@Composable
fun rememberBase64Image(base64Str: String?): ImageBitmap? {
    if (base64Str == null) return null
    return remember(base64Str) {
        try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandApp(viewModel: BrandViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Collect States
    val productName by viewModel.productName.collectAsStateWithLifecycle()
    val productDescription by viewModel.productDescription.collectAsStateWithLifecycle()
    val brandTheme by viewModel.brandTheme.collectAsStateWithLifecycle()
    val brandColors by viewModel.brandColors.collectAsStateWithLifecycle()
    val generationStatus by viewModel.generationStatus.collectAsStateWithLifecycle()
    val allCampaigns by viewModel.allCampaigns.collectAsStateWithLifecycle()
    val currentViewingCampaign by viewModel.currentViewingCampaign.collectAsStateWithLifecycle()

    var showCampaignDetailsDialog by remember { mutableStateOf<BrandCampaign?>(null) }
    var currentMediumPreview by remember { mutableStateOf<Pair<String, String>?>(null) } // mediumName, base64

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CharcoalDark,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Campaign History Logo",
                            tint = BorderGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Campaign Archive",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LightGold
                        )
                    }

                    HorizontalDivider(color = CardBackground, modifier = Modifier.padding(bottom = 8.dp))

                    if (allCampaigns.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                text = "No saved brands yet.\nGenerate assets to save!",
                                color = SlateText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("campaign_history_list")
                        ) {
                            items(allCampaigns) { campaign ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (currentViewingCampaign?.id == campaign.id) {
                                            Color(0xFF2C2518)
                                        } else {
                                            CardBackground
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (currentViewingCampaign?.id == campaign.id) BorderGold else Color.Transparent
                                    ),
                                    onClick = {
                                        viewModel.selectCampaign(campaign)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = campaign.productName,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = campaign.brandTheme,
                                                fontSize = 12.sp,
                                                color = BorderGold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Colors: ${campaign.brandColors}",
                                                fontSize = 11.sp,
                                                color = SlateText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteCampaign(campaign) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete product campaign",
                                                tint = Color(0xFFEF5350),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.resetInputs()
                            scope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, SlateText.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Campaign Icon", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Start Fresh Brand", color = Color.White)
                    }
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BRAND",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BUILDER",
                                    color = BorderGold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CharcoalDark,
                            titleContentColor = Color.White
                        ),
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open campaign archive drawer",
                                    tint = BorderGold
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.resetInputs() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Clear fields and reset application state",
                                    tint = SlateText
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Surface(
                    color = CharcoalDark,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Grand Banner Decor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .drawBehind {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                AccentGoldDark.copy(alpha = 0.25f),
                                                CharcoalDark
                                            )
                                        )
                                    )
                                    // Custom visual asset design lines
                                    drawLine(
                                        color = BorderGold.copy(alpha = 0.2f),
                                        start = Offset(0f, size.height * 0.1f),
                                        end = Offset(size.width, size.height * 0.9f),
                                        strokeWidth = 2f
                                    )
                                    drawLine(
                                        color = BorderGold.copy(alpha = 0.15f),
                                        start = Offset(0f, size.height * 0.6f),
                                        end = Offset(size.width, size.height * 0.2f),
                                        strokeWidth = 1f
                                    )
                                }
                                .padding(24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BorderGold.copy(alpha = 0.15f))
                                        .border(1.dp, BorderGold.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "NANO-BANANA ENGINE ACTIVE",
                                        color = BorderGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Imagine Your Product",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Describe your vision and render consistent, stunning branding across physical and digital mediums.",
                                    fontSize = 12.sp,
                                    color = SlateText,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Main Inputs Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .border(1.dp, BorderGold.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "PRODUCT CONTEXT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BorderGold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Product Name Input
                                OutlinedTextField(
                                    value = productName,
                                    onValueChange = { viewModel.productName.value = it },
                                    label = { Text("Product Name") },
                                    placeholder = { Text("e.g. Ascent Flask, Eon Watch") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BorderGold,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedLabelColor = BorderGold,
                                        unfocusedLabelColor = SlateText,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("product_name_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Product Description Input
                                OutlinedTextField(
                                    value = productDescription,
                                    onValueChange = { viewModel.productDescription.value = it },
                                    label = { Text("Product Description") },
                                    placeholder = { Text("Describe the product physical looks, materials, finish, specific color style, detailing...") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BorderGold,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedLabelColor = BorderGold,
                                        unfocusedLabelColor = SlateText,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .testTag("product_desc_input")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Brand Theme Select Box (We can provide standard options)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Branding Style Theme",
                                            color = SlateText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        ThemeRadioSelector(
                                            selectedTheme = brandTheme,
                                            onThemeSelected = { viewModel.brandTheme.value = it }
                                        )
                                    }

                                    // Brand Colors Input
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Core Palette Colors",
                                            color = SlateText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        OutlinedTextField(
                                            value = brandColors,
                                            onValueChange = { viewModel.brandColors.value = it },
                                            placeholder = { Text("e.g. Copper & Sage") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BorderGold,
                                                unfocusedBorderColor = Color.DarkGray,
                                                focusedLabelColor = BorderGold,
                                                unfocusedLabelColor = SlateText,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Consistency Guidance Badge / No People
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, BorderGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Policy Compliance",
                                        tint = BorderGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Consistency locked. No humans/people will be generated in shots.",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Generate Campaign Button
                                val isGeneratingAll = generationStatus.billboardState is GenerationState.Generating ||
                                        generationStatus.newspaperState is GenerationState.Generating ||
                                        generationStatus.socialPostState is GenerationState.Generating

                                Button(
                                    onClick = { viewModel.startCampaignGeneration() },
                                    enabled = !isGeneratingAll,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BorderGold,
                                        contentColor = Color.Black,
                                        disabledContainerColor = Color.DarkGray,
                                        disabledContentColor = Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("generate_campaign_button")
                                ) {
                                    if (isGeneratingAll) {
                                        CircularProgressIndicator(
                                            color = Color.Black,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Rendering Brand Universe...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    } else {
                                        Text(
                                            text = if (allCampaigns.any { it.productName == productName }) "Re-Image Global Campaign" else "Visualize Across Mediums",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                generationStatus.errorSummary?.let { error ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Render Workspace / Medium Results
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "CAMPAIGN MEDIUM WORKSPACE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BorderGold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // 1. Billboard Card Mockup (16:9)
                            MediumMockupCard(
                                title = "Outdoor Billboard Campaign",
                                subtitle = "Medium Aspect: 16:9 • High-Contrast Wide Angle",
                                state = generationStatus.billboardState,
                                placeholderDesc = "Imagine your product commanding attention on an elegant concrete freeway frame in downtown Tokyo.",
                                containerRatio = 16f / 9f,
                                onRegenerate = { viewModel.regenerateMedium("billboard") },
                                onViewFull = { base64 -> currentMediumPreview = Pair("Billboard Campaign", base64) },
                                customImageFrame = { imageBitmap ->
                                    // Render billboard frame!
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black)
                                    ) {
                                        Image(
                                            bitmap = imageBitmap,
                                            contentDescription = "Billboard Generated Image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Subtle billboard top lighting mock overlay
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.TopCenter)
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            repeat(4) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 16.dp, height = 4.dp)
                                                        .clip(RoundedCornerShape(1f))
                                                        .background(Color.White.copy(alpha = 0.7f))
                                                )
                                            }
                                        }
                                        // Label overlay
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Metropolitan Billboard Mock",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2. Newspaper Card Mockup (3:4)
                            MediumMockupCard(
                                title = "Editorial Newsprint Placement",
                                subtitle = "Medium Aspect: 3:4 • Editorial Type Layout",
                                state = generationStatus.newspaperState,
                                placeholderDesc = "Visualize your product on clean cream newspaper spreads, paired with rich technical copywriting and structured margins.",
                                containerRatio = 3f / 4f,
                                onRegenerate = { viewModel.regenerateMedium("newspaper") },
                                onViewFull = { base64 -> currentMediumPreview = Pair("Editorial Newsprint", base64) },
                                customImageFrame = { imageBitmap ->
                                    // Newsprint layout!
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFEFECE4)) // Antique Newsprint background color
                                            .border(4.dp, Color(0xFFC7C0B0))
                                    ) {
                                        // Fake editorial header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "THE CHRONICLE ADVERTISER",
                                                color = Color(0xFF3E3A33),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Serif
                                            )
                                            Text(
                                                text = "EST. 1821",
                                                color = Color(0xFF3E3A33),
                                                fontSize = 6.sp,
                                                fontFamily = FontFamily.Serif
                                            )
                                        }
                                        HorizontalDivider(color = Color(0xFF3E3A33), thickness = 1.dp, modifier = Modifier.padding(horizontal = 6.dp))
                                        
                                        // The image box inside newspaper
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .padding(6.dp)
                                                .background(Color.White)
                                                .border(0.5.dp, Color(0xFF3E3A33))
                                        ) {
                                            Image(
                                                bitmap = imageBitmap,
                                                contentDescription = "Newspaper Generated Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Mock editorial captions/copy columns
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "THE REVELATION OF FORM",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.sp,
                                                    color = Color(0xFF3E3A33),
                                                    fontFamily = FontFamily.Serif
                                                )
                                                Text(
                                                    text = "In an era of disposable noise, craftsmanship makes its ultimate return to grace. This campaign illustrates how product consistency...",
                                                    fontSize = 6.sp,
                                                    color = Color(0xFF4E4A43),
                                                    lineHeight = 8.sp,
                                                    fontFamily = FontFamily.Serif
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "COHERENT BRAND IDENTITY",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.sp,
                                                    color = Color(0xFF3E3A33),
                                                    fontFamily = FontFamily.Serif
                                                )
                                                Text(
                                                    text = "No human figures clutter the visual canvas. Pure geometry, intentional lighting, and elite materials define our modern outlook...",
                                                    fontSize = 6.sp,
                                                    color = Color(0xFF4E4A43),
                                                    lineHeight = 8.sp,
                                                    fontFamily = FontFamily.Serif
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3. Social Post Card Mockup (1:1)
                            MediumMockupCard(
                                title = "Digital Social Campaign",
                                subtitle = "Medium Aspect: 1:1 • Modern Aesthetic Square",
                                state = generationStatus.socialPostState,
                                placeholderDesc = "Render a gorgeous Instagram lifestyle placement characterized by sophisticated shadows, minimal backgrounds, and pure aesthetic weight.",
                                containerRatio = 1f,
                                onRegenerate = { viewModel.regenerateMedium("social post") },
                                onViewFull = { base64 -> currentMediumPreview = Pair("Social Post Campaign", base64) },
                                customImageFrame = { imageBitmap ->
                                    // Social post layout!
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(CardBackground)
                                    ) {
                                        // Post header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(BorderGold)
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (productName.isNotEmpty()) productName.take(1).uppercase() else "B",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (productName.isNotEmpty()) productName.lowercase() else "brandbuilder",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Sponsored",
                                                    color = SlateText,
                                                    fontSize = 8.sp
                                                )
                                            }
                                        }

                                        // The image box inside social
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .background(Color.Black)
                                        ) {
                                            Image(
                                                bitmap = imageBitmap,
                                                contentDescription = "Social Generated Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Post social action bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Liked",
                                                tint = BorderGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Details",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(BorderGold)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Learn More",
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // Image Zoom & Prompt Detail Viewer Dialog
            currentMediumPreview?.let { pair ->
                val (title, base64) = pair
                val imageBitmap = rememberBase64Image(base64)
                Dialog(onDismissRequest = { currentMediumPreview = null }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, BorderGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = BorderGold,
                                fontSize = 14.sp,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f) // Square box for dialog presentation
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                if (imageBitmap != null) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = "Inspected Campaign Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        color = BorderGold,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Brand Consistency Details:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This visual is fully generated using the Nano-Banana image engine. All human features are suppressed. Product consistency is enforced by propagating shared material attributes and geometric blueprints, focusing heavily on brand aesthetic colors: $brandColors.",
                                color = SlateText,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { currentMediumPreview = null },
                                colors = ButtonDefaults.buttonColors(containerColor = BorderGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Close Deck View")
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ThemeRadioSelector(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val themes = listOf("Minimalist & Clean", "Luxury & Bold", "Futuristic/Tech", "Warm & Organic")
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.15f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedTheme,
                color = Color.White,
                fontSize = 14.sp
            )
            Icon(
                imageVector = if (expanded) Icons.Default.CheckCircle else Icons.Default.Menu,
                contentDescription = "Expand brand themes",
                tint = BorderGold,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(CardBackground)
                .border(1.dp, BorderGold.copy(alpha = 0.3f))
        ) {
            themes.forEach { theme ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = theme,
                            color = if (theme == selectedTheme) BorderGold else Color.White,
                            fontWeight = if (theme == selectedTheme) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onThemeSelected(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MediumMockupCard(
    title: String,
    subtitle: String,
    state: GenerationState,
    placeholderDesc: String,
    containerRatio: Float,
    onRegenerate: () -> Unit,
    onViewFull: (String) -> Unit,
    customImageFrame: @Composable (ImageBitmap) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, BorderGold.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = SlateText
                    )
                }

                if (state is GenerationState.Success) {
                    IconButton(onClick = onRegenerate) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate single medium",
                            tint = BorderGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Aspect Ratio Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(containerRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(
                        BorderStroke(
                            1.dp,
                            if (state is GenerationState.Generating) BorderGold else Color.DarkGray
                        ),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                when (state) {
                    is GenerationState.Idle -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Asset Pending Visual Guide",
                                tint = SlateText.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = placeholderDesc,
                                color = SlateText,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    is GenerationState.Generating -> {
                        // Pulse Animation for Generating Screen
                        val transition = rememberInfiniteTransition(label = "pulse")
                        val alpha by transition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.7f,
                            animationSpec = infiniteRepeatable(
                                animation = keyframes { durationMillis = 1000 },
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    drawRect(color = BorderGold.copy(alpha = alpha))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = BorderGold,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Generating Image...",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is GenerationState.Success -> {
                        val imageBitmap = rememberBase64Image(state.base64)
                        if (imageBitmap != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                customImageFrame(imageBitmap)
                                // Top right detail zoom overlay
                                IconButton(
                                    onClick = { onViewFull(state.base64) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Expand visual details",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Error decoding image.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    is GenerationState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Processing failed",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = onRegenerate,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Retry", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
