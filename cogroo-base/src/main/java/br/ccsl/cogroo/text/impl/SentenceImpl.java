package br.ccsl.cogroo.text.impl;

import java.util.List;

import opennlp.tools.util.Span;

import br.ccsl.cogroo.text.Chunk;
import br.ccsl.cogroo.text.Document;
import br.ccsl.cogroo.text.Sentence;
import br.ccsl.cogroo.text.SyntacticChunk;
import br.ccsl.cogroo.text.Token;

import com.google.common.base.Objects;

/**
 * The <code>Sentence</code> class contains the position of the sentence in the
 * text and the list of word in it.
 */
public class SentenceImpl implements Sentence {

  /** the position of the sentence in the text */
  private Span span;

  /** the list every token in the sentence */
  private List<Token> tokens;
  
  private List<Chunk> chunks;
  
  private List<SyntacticChunk> syntacticChunks;
  
  /* a reference to the document that contains this sentence */
  private Document theDocument;
  
  public SentenceImpl(Span span, Document theDocument) {
    this(span, null, theDocument);
  }

  public SentenceImpl(Span span, List<Token> tokens, Document theDocument) {
    this.span = span;
    this.tokens = tokens;
    this.theDocument = theDocument;
  }

  /* (non-Javadoc)
   * @see br.ccsl.cogroo.text.Sentence#getText()
   */
  public String getText() {
    return span.getCoveredText(theDocument.getText()).toString();
  }

  /* (non-Javadoc)
   * @see br.ccsl.cogroo.text.Sentence#getSpan()
   */
  public Span getSpan() {
    return span;
  }

  /* (non-Javadoc)
   * @see br.ccsl.cogroo.text.Sentence#setSpan(opennlp.tools.util.Span)
   */
  public void setSpan(Span span) {
    this.span = span;
  }

  /* (non-Javadoc)
   * @see br.ccsl.cogroo.text.Sentence#getTokens()
   */
  public List<Token> getTokens() {
    return tokens;
  }

  /* (non-Javadoc)
   * @see br.ccsl.cogroo.text.Sentence#setTokens(java.util.List)
   */
  public void setTokens(List<Token> tokens) {
    this.tokens = tokens;
  }
  
  public List<Chunk> getChunks() {
    return chunks;
  }

  public void setChunks(List<Chunk> chunks) {
    this.chunks = chunks;
  }
  
  public List<SyntacticChunk> getSyntacticChunks() {
    return syntacticChunks;
  }

  public void setSyntacticChunks(List<SyntacticChunk> syntacticChunks) {
    this.syntacticChunks = syntacticChunks;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SentenceImpl) {
      SentenceImpl that = (SentenceImpl) obj;
      return Objects.equal(this.tokens, that.tokens)
          && Objects.equal(this.span, that.span);
    }
    return false;
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("span", span).add("tk", tokens)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(span, tokens);
  }

}