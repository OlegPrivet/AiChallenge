package org.oleg.ai.challenge.data.rag.search

import co.touchlab.kermit.Logger
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.store.MMapDirectory
import org.oleg.ai.challenge.domain.rag.lexical.LexicalMatch
import org.oleg.ai.challenge.domain.rag.lexical.LexicalSearchService
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import java.nio.file.Paths
import org.oleg.ai.challenge.domain.rag.model.Document as DomainDocument

private const val FIELD_DOCUMENT_ID = "documentId"
private const val FIELD_CHUNK_ID = "chunkId"
private const val FIELD_CHUNK_INDEX = "chunkIndex"
private const val FIELD_CONTENT = "content"

class LuceneBm25SearchService(
    indexPath: String? = null,
    private val logger: Logger = Logger.withTag("LuceneBm25SearchService")
) : LexicalSearchService {

    private val analyzer = StandardAnalyzer()
    private val directory = MMapDirectory(
        Paths.get(indexPath ?: defaultIndexPath())
    ).also { dir ->
        logger.i { "Initialized BM25 index at: ${dir.directory}" }
    }
    private val writer: IndexWriter

    init {
        val config = IndexWriterConfig(analyzer).apply {
            similarity = BM25Similarity()
        }
        writer = IndexWriter(directory, config)
    }

    override suspend fun index(document: DomainDocument, chunks: List<DocumentChunk>) {
        if (chunks.isEmpty()) return
        chunks.forEach { chunk ->
            writer.updateDocument(
                Term(FIELD_CHUNK_ID, chunk.id),
                createLuceneDocument(document, chunk)
            )
        }
        writer.commit()
        logger.d { "Indexed ${chunks.size} chunks for document=${document.id}" }
    }

    override suspend fun delete(documentId: String) {
        writer.deleteDocuments(Term(FIELD_DOCUMENT_ID, documentId))
        writer.commit()
        logger.d { "Deleted document=$documentId from BM25 index" }
    }

    override suspend fun search(
        query: String,
        topK: Int,
        filters: Map<String, String>
    ): List<LexicalMatch> {
        val reader = DirectoryReader.open(writer)
        val searcher = IndexSearcher(reader).apply {
            similarity = BM25Similarity()
        }

        val parsedQuery = buildQuery(query, filters)
        val hits = searcher.search(parsedQuery, topK).scoreDocs

        val results = hits.mapNotNull { hit ->
            toLexicalMatch(searcher, hit)
        }
        reader.close()
        return results
    }

    private fun buildQuery(query: String, filters: Map<String, String>): Query {
        val parser = QueryParser(FIELD_CONTENT, analyzer)
        val baseQuery = parser.parse(query.ifBlank { "*" })

        if (filters.isEmpty()) return baseQuery

        val builder = BooleanQuery.Builder()
        builder.add(baseQuery, BooleanClause.Occur.MUST)
        filters.forEach { (key, value) ->
            builder.add(TermQuery(Term(key, value)), BooleanClause.Occur.FILTER)
        }
        return builder.build()
    }

    private fun createLuceneDocument(document: DomainDocument, chunk: DocumentChunk): Document {
        return Document().apply {
            add(StringField(FIELD_DOCUMENT_ID, document.id, Field.Store.YES))
            add(StringField(FIELD_CHUNK_ID, chunk.id, Field.Store.YES))
            add(StringField(FIELD_CHUNK_INDEX, chunk.chunkIndex.toString(), Field.Store.YES))
            add(TextField(FIELD_CONTENT, chunk.content, Field.Store.YES))
            add(StringField("sourceType", document.sourceType.name, Field.Store.NO))
            add(StringField("chunkingVersion", chunk.chunkingStrategyVersion, Field.Store.NO))
            add(StringField("embeddingVersion", chunk.embeddingModelVersion, Field.Store.NO))
            document.metadata.forEach { (k, v) ->
                add(StringField(k, v, Field.Store.NO))
            }
        }
    }

    private fun toLexicalMatch(searcher: IndexSearcher, hit: ScoreDoc): LexicalMatch? {
        val doc = searcher.indexReader.storedFields().document(hit.doc)
        val documentId = doc.get(FIELD_DOCUMENT_ID) ?: return null
        val chunkId = doc.get(FIELD_CHUNK_ID) ?: return null
        val chunkIndex = doc.get(FIELD_CHUNK_INDEX)?.toIntOrNull()
        return LexicalMatch(
            chunkId = chunkId,
            documentId = documentId,
            chunkIndex = chunkIndex,
            score = hit.score.toDouble()
        )
    }

    /**
     * Closes the index writer and directory, releasing all resources.
     * Should be called when the service is no longer needed.
     */
    fun close() {
        writer.close()
        directory.close()
        logger.i { "Closed BM25 index" }
    }

    private fun defaultIndexPath(): String {
        val base = System.getProperty("java.io.tmpdir") ?: "."
        return Paths.get(base, "rag-bm25-index").toString()
    }
}
