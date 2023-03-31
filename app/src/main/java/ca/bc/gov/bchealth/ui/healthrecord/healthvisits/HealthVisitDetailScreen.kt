package ca.bc.gov.bchealth.ui.healthrecord.healthvisits

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.compose.MyHealthTypography
import ca.bc.gov.bchealth.ui.comment.Comment
import ca.bc.gov.bchealth.ui.comment.CommentsSummaryUI
import ca.bc.gov.bchealth.ui.custom.MyHealthClickableText
import ca.bc.gov.bchealth.ui.healthrecord.HealthRecordDetailItem
import ca.bc.gov.bchealth.ui.healthrecord.HealthRecordListItem
import ca.bc.gov.bchealth.widget.CommentInputUI
import java.time.Instant

@Composable
fun BoxScope.HealthVisitDetailScreen(
    uiState: HealthVisitDetailUiState,
    onClickFaq: () -> Unit,
    onClickComments: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.TopCenter),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            MyHealthClickableText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 32.dp, end = 32.dp),
                style = MyHealthTypography.caption,
                fullText = stringResource(id = R.string.information_is_from_the_billing_claim),
                clickableText = stringResource(id = R.string.faq),
                action = onClickFaq
            )

            uiState.uiList.forEach { listItem ->
                HealthRecordListItem(
                    stringResource(id = listItem.title),
                    listItem.description.orEmpty(),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val date = Instant.now()
            val comment = Comment(
                text = "This is a long comment that should break the line",
                date = date,
                version = 0L,
                isUploaded = true,
                entryTypeCode = "",
                createdBy = "",
                createdDateTime = date,
                updatedDateTime = date,
                updatedBy = ""
            )

            CommentsSummaryUI(count = 5, lastComment = comment, onClickComments)
        }

        CommentInputUI(onSubmitComment = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewHealthVisitDetailContent() {
    Box {
        HealthVisitDetailScreen(
            HealthVisitDetailUiState(
                uiList = listOf(
                    HealthRecordDetailItem(
                        R.string.clinic_name,
                        "FRANCIS N WER"
                    ),
                    HealthRecordDetailItem(
                        R.string.practitioner_name,
                        "Daniel Something"
                    )
                )
            ),
            {}, {}
        )
    }
}
