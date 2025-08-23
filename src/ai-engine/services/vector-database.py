"""
Vector Database Integration - Advanced Pattern Storage and Retrieval
High-performance vector search for learning patterns and AI-powered recommendations
"""

import asyncio
import logging
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime
import json
import numpy as np
from dataclasses import dataclass, asdict
import hashlib
import pickle
from abc import ABC, abstractmethod

# Vector database libraries
import pinecone
import weaviate
import qdrant_client
from qdrant_client.models import Distance, VectorParams, PointStruct

# Embedding models
import openai
from sentence_transformers import SentenceTransformer
from transformers import AutoTokenizer, AutoModel
import torch

# Utilities
import faiss
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.decomposition import PCA


class VectorDBType(str):
    PINECONE = "pinecone"
    WEAVIATE = "weaviate" 
    QDRANT = "qdrant"
    FAISS = "faiss"


@dataclass
class VectorRecord:
    id: str
    vector: List[float]
    metadata: Dict[str, Any]
    timestamp: datetime
    collection: str
    confidence_score: float = 1.0


@dataclass
class SearchResult:
    id: str
    score: float
    metadata: Dict[str, Any]
    vector: Optional[List[float]] = None


@dataclass
class PatternVector:
    pattern_id: str
    pattern_type: str
    content_embedding: List[float]
    metadata_embedding: List[float]
    combined_embedding: List[float]
    metadata: Dict[str, Any]
    performance_metrics: Dict[str, float]
    created_at: datetime
    last_accessed: datetime
    access_count: int


class VectorDatabase(ABC):
    """Abstract base class for vector database implementations"""
    
    @abstractmethod
    async def connect(self) -> bool:
        """Connect to vector database"""
        pass
    
    @abstractmethod
    async def create_collection(self, name: str, dimension: int) -> bool:
        """Create a collection/index"""
        pass
    
    @abstractmethod
    async def upsert_vectors(self, vectors: List[VectorRecord]) -> bool:
        """Insert or update vectors"""
        pass
    
    @abstractmethod
    async def search(
        self, 
        query_vector: List[float], 
        collection: str,
        top_k: int = 10,
        filter_criteria: Optional[Dict] = None
    ) -> List[SearchResult]:
        """Search for similar vectors"""
        pass
    
    @abstractmethod
    async def delete_vectors(self, ids: List[str], collection: str) -> bool:
        """Delete vectors by IDs"""
        pass
    
    @abstractmethod
    async def get_collection_stats(self, collection: str) -> Dict[str, Any]:
        """Get collection statistics"""
        pass


class PineconeVectorDB(VectorDatabase):
    """Pinecone vector database implementation"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("pinecone_db")
        self.client = None
        self.indexes = {}
    
    async def connect(self) -> bool:
        """Connect to Pinecone"""
        try:
            pinecone.init(
                api_key=self.config['api_key'],
                environment=self.config['environment']
            )
            self.client = pinecone
            self.logger.info("Connected to Pinecone successfully")
            return True
        except Exception as e:
            self.logger.error(f"Failed to connect to Pinecone: {e}")
            return False
    
    async def create_collection(self, name: str, dimension: int) -> bool:
        """Create Pinecone index"""
        try:
            if name not in pinecone.list_indexes():
                pinecone.create_index(
                    name=name,
                    dimension=dimension,
                    metric='cosine',
                    pods=1,
                    replicas=1,
                    pod_type='p1.x1'
                )
                self.logger.info(f"Created Pinecone index: {name}")
            
            self.indexes[name] = pinecone.Index(name)
            return True
        except Exception as e:
            self.logger.error(f"Failed to create Pinecone index {name}: {e}")
            return False
    
    async def upsert_vectors(self, vectors: List[VectorRecord]) -> bool:
        """Upsert vectors to Pinecone"""
        try:
            grouped_vectors = {}
            
            # Group vectors by collection
            for vector in vectors:
                if vector.collection not in grouped_vectors:
                    grouped_vectors[vector.collection] = []
                grouped_vectors[vector.collection].append(vector)
            
            # Upsert to each collection
            for collection, col_vectors in grouped_vectors.items():
                if collection not in self.indexes:
                    await self.create_collection(collection, len(col_vectors[0].vector))
                
                # Prepare data for Pinecone
                upsert_data = [
                    (v.id, v.vector, v.metadata)
                    for v in col_vectors
                ]
                
                # Batch upsert
                batch_size = 100
                for i in range(0, len(upsert_data), batch_size):
                    batch = upsert_data[i:i + batch_size]
                    self.indexes[collection].upsert(batch)
            
            self.logger.info(f"Upserted {len(vectors)} vectors")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to upsert vectors: {e}")
            return False
    
    async def search(
        self,
        query_vector: List[float],
        collection: str,
        top_k: int = 10,
        filter_criteria: Optional[Dict] = None
    ) -> List[SearchResult]:
        """Search Pinecone index"""
        try:
            if collection not in self.indexes:
                return []
            
            response = self.indexes[collection].query(
                vector=query_vector,
                top_k=top_k,
                filter=filter_criteria,
                include_metadata=True
            )
            
            results = []
            for match in response.matches:
                result = SearchResult(
                    id=match.id,
                    score=match.score,
                    metadata=match.metadata
                )
                results.append(result)
            
            return results
            
        except Exception as e:
            self.logger.error(f"Search failed: {e}")
            return []
    
    async def delete_vectors(self, ids: List[str], collection: str) -> bool:
        """Delete vectors from Pinecone"""
        try:
            if collection not in self.indexes:
                return False
            
            self.indexes[collection].delete(ids=ids)
            return True
        except Exception as e:
            self.logger.error(f"Failed to delete vectors: {e}")
            return False
    
    async def get_collection_stats(self, collection: str) -> Dict[str, Any]:
        """Get Pinecone index stats"""
        try:
            if collection not in self.indexes:
                return {}
            
            stats = self.indexes[collection].describe_index_stats()
            return stats
        except Exception as e:
            self.logger.error(f"Failed to get stats: {e}")
            return {}


class QdrantVectorDB(VectorDatabase):
    """Qdrant vector database implementation"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("qdrant_db")
        self.client = None
    
    async def connect(self) -> bool:
        """Connect to Qdrant"""
        try:
            self.client = qdrant_client.QdrantClient(
                host=self.config.get('host', 'localhost'),
                port=self.config.get('port', 6333),
                api_key=self.config.get('api_key')
            )
            self.logger.info("Connected to Qdrant successfully")
            return True
        except Exception as e:
            self.logger.error(f"Failed to connect to Qdrant: {e}")
            return False
    
    async def create_collection(self, name: str, dimension: int) -> bool:
        """Create Qdrant collection"""
        try:
            self.client.create_collection(
                collection_name=name,
                vectors_config=VectorParams(
                    size=dimension,
                    distance=Distance.COSINE
                )
            )
            self.logger.info(f"Created Qdrant collection: {name}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to create collection {name}: {e}")
            return False
    
    async def upsert_vectors(self, vectors: List[VectorRecord]) -> bool:
        """Upsert vectors to Qdrant"""
        try:
            grouped_vectors = {}
            
            for vector in vectors:
                if vector.collection not in grouped_vectors:
                    grouped_vectors[vector.collection] = []
                grouped_vectors[vector.collection].append(vector)
            
            for collection, col_vectors in grouped_vectors.items():
                points = [
                    PointStruct(
                        id=v.id,
                        vector=v.vector,
                        payload=v.metadata
                    )
                    for v in col_vectors
                ]
                
                self.client.upsert(
                    collection_name=collection,
                    points=points
                )
            
            return True
        except Exception as e:
            self.logger.error(f"Failed to upsert vectors: {e}")
            return False
    
    async def search(
        self,
        query_vector: List[float],
        collection: str,
        top_k: int = 10,
        filter_criteria: Optional[Dict] = None
    ) -> List[SearchResult]:
        """Search Qdrant collection"""
        try:
            search_result = self.client.search(
                collection_name=collection,
                query_vector=query_vector,
                limit=top_k,
                query_filter=filter_criteria
            )
            
            results = []
            for point in search_result:
                result = SearchResult(
                    id=str(point.id),
                    score=point.score,
                    metadata=point.payload
                )
                results.append(result)
            
            return results
        except Exception as e:
            self.logger.error(f"Search failed: {e}")
            return []
    
    async def delete_vectors(self, ids: List[str], collection: str) -> bool:
        """Delete vectors from Qdrant"""
        try:
            self.client.delete(
                collection_name=collection,
                points_selector=ids
            )
            return True
        except Exception as e:
            self.logger.error(f"Failed to delete vectors: {e}")
            return False
    
    async def get_collection_stats(self, collection: str) -> Dict[str, Any]:
        """Get Qdrant collection info"""
        try:
            info = self.client.get_collection(collection)
            return {
                'vector_count': info.points_count,
                'status': info.status,
                'optimizer_status': info.optimizer_status
            }
        except Exception as e:
            self.logger.error(f"Failed to get stats: {e}")
            return {}


class FAISSVectorDB(VectorDatabase):
    """FAISS local vector database implementation"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("faiss_db")
        self.indexes = {}
        self.metadata_stores = {}
        self.id_maps = {}
    
    async def connect(self) -> bool:
        """Connect to FAISS (local)"""
        try:
            # FAISS is local, just initialize
            self.logger.info("FAISS database initialized")
            return True
        except Exception as e:
            self.logger.error(f"Failed to initialize FAISS: {e}")
            return False
    
    async def create_collection(self, name: str, dimension: int) -> bool:
        """Create FAISS index"""
        try:
            # Create FAISS index
            index = faiss.IndexFlatIP(dimension)  # Inner product (cosine similarity)
            
            # Wrap with IDMap for string IDs
            index = faiss.IndexIDMap(index)
            
            self.indexes[name] = index
            self.metadata_stores[name] = {}
            self.id_maps[name] = {}
            
            self.logger.info(f"Created FAISS index: {name}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to create FAISS index {name}: {e}")
            return False
    
    async def upsert_vectors(self, vectors: List[VectorRecord]) -> bool:
        """Upsert vectors to FAISS"""
        try:
            grouped_vectors = {}
            
            for vector in vectors:
                if vector.collection not in grouped_vectors:
                    grouped_vectors[vector.collection] = []
                grouped_vectors[vector.collection].append(vector)
            
            for collection, col_vectors in grouped_vectors.items():
                if collection not in self.indexes:
                    await self.create_collection(collection, len(col_vectors[0].vector))
                
                # Prepare vectors and IDs
                vectors_array = np.array([v.vector for v in col_vectors]).astype('float32')
                
                # Generate numeric IDs for FAISS
                numeric_ids = []
                for v in col_vectors:
                    if v.id in self.id_maps[collection]:
                        numeric_id = self.id_maps[collection][v.id]
                    else:
                        numeric_id = len(self.id_maps[collection])
                        self.id_maps[collection][v.id] = numeric_id
                        self.id_maps[collection][numeric_id] = v.id  # Reverse mapping
                    
                    numeric_ids.append(numeric_id)
                    self.metadata_stores[collection][v.id] = v.metadata
                
                # Add to index
                self.indexes[collection].add_with_ids(
                    vectors_array, 
                    np.array(numeric_ids).astype('int64')
                )
            
            return True
        except Exception as e:
            self.logger.error(f"Failed to upsert vectors: {e}")
            return False
    
    async def search(
        self,
        query_vector: List[float],
        collection: str,
        top_k: int = 10,
        filter_criteria: Optional[Dict] = None
    ) -> List[SearchResult]:
        """Search FAISS index"""
        try:
            if collection not in self.indexes:
                return []
            
            query_array = np.array([query_vector]).astype('float32')
            scores, indices = self.indexes[collection].search(query_array, top_k)
            
            results = []
            for score, idx in zip(scores[0], indices[0]):
                if idx == -1:  # No more results
                    continue
                
                # Get string ID from numeric ID
                string_id = self.id_maps[collection].get(idx)
                if string_id is None:
                    continue
                
                metadata = self.metadata_stores[collection].get(string_id, {})
                
                # Apply filter if provided
                if filter_criteria:
                    if not self._matches_filter(metadata, filter_criteria):
                        continue
                
                result = SearchResult(
                    id=string_id,
                    score=float(score),
                    metadata=metadata
                )
                results.append(result)
            
            return results
        except Exception as e:
            self.logger.error(f"Search failed: {e}")
            return []
    
    def _matches_filter(self, metadata: Dict, filter_criteria: Dict) -> bool:
        """Check if metadata matches filter criteria"""
        for key, value in filter_criteria.items():
            if key not in metadata:
                return False
            if metadata[key] != value:
                return False
        return True
    
    async def delete_vectors(self, ids: List[str], collection: str) -> bool:
        """Delete vectors from FAISS (not supported directly)"""
        # FAISS doesn't support deletion directly, would need to rebuild
        self.logger.warning("FAISS doesn't support direct deletion")
        return False
    
    async def get_collection_stats(self, collection: str) -> Dict[str, Any]:
        """Get FAISS index stats"""
        try:
            if collection not in self.indexes:
                return {}
            
            return {
                'vector_count': self.indexes[collection].ntotal,
                'dimension': self.indexes[collection].d,
                'is_trained': self.indexes[collection].is_trained
            }
        except Exception as e:
            self.logger.error(f"Failed to get stats: {e}")
            return {}


class EmbeddingService:
    """Service for generating embeddings from text and data"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("embedding_service")
        
        # Initialize embedding models
        self.openai_client = openai
        self.openai_client.api_key = config.get('openai_api_key')
        
        # Sentence Transformer model
        self.sentence_model = SentenceTransformer(
            config.get('sentence_model', 'all-MiniLM-L6-v2')
        )
        
        # Custom transformer model (optional)
        self.custom_model = None
        if config.get('custom_model_path'):
            self.custom_model = AutoModel.from_pretrained(config['custom_model_path'])
            self.custom_tokenizer = AutoTokenizer.from_pretrained(config['custom_model_path'])
    
    async def embed_text(self, text: str, model: str = "openai") -> List[float]:
        """Generate text embedding using specified model"""
        try:
            if model == "openai":
                return await self._embed_with_openai(text)
            elif model == "sentence_transformer":
                return self._embed_with_sentence_transformer(text)
            elif model == "custom":
                return await self._embed_with_custom_model(text)
            else:
                # Default to sentence transformer
                return self._embed_with_sentence_transformer(text)
        except Exception as e:
            self.logger.error(f"Embedding generation failed: {e}")
            return []
    
    async def _embed_with_openai(self, text: str) -> List[float]:
        """Generate embedding using OpenAI API"""
        try:
            response = await self.openai_client.embeddings.create(
                input=text,
                model="text-embedding-ada-002"
            )
            return response.data[0].embedding
        except Exception as e:
            self.logger.error(f"OpenAI embedding failed: {e}")
            return []
    
    def _embed_with_sentence_transformer(self, text: str) -> List[float]:
        """Generate embedding using Sentence Transformers"""
        try:
            embedding = self.sentence_model.encode(text)
            return embedding.tolist()
        except Exception as e:
            self.logger.error(f"Sentence Transformer embedding failed: {e}")
            return []
    
    async def _embed_with_custom_model(self, text: str) -> List[float]:
        """Generate embedding using custom transformer model"""
        try:
            if self.custom_model is None:
                return []
            
            inputs = self.custom_tokenizer(text, return_tensors="pt", truncation=True, padding=True)
            
            with torch.no_grad():
                outputs = self.custom_model(**inputs)
                # Use mean pooling
                embeddings = outputs.last_hidden_state.mean(dim=1)
                return embeddings[0].tolist()
        except Exception as e:
            self.logger.error(f"Custom model embedding failed: {e}")
            return []
    
    async def embed_pattern(self, pattern_data: Dict[str, Any]) -> PatternVector:
        """Generate comprehensive embedding for a learning pattern"""
        try:
            # Extract text content for embedding
            content_text = self._extract_content_text(pattern_data)
            metadata_text = self._extract_metadata_text(pattern_data)
            
            # Generate embeddings
            content_embedding = await self.embed_text(content_text, "openai")
            metadata_embedding = await self.embed_text(metadata_text, "sentence_transformer")
            
            # Combine embeddings (concatenation + dimensionality reduction)
            combined_embedding = self._combine_embeddings(content_embedding, metadata_embedding)
            
            return PatternVector(
                pattern_id=pattern_data.get('pattern_id', ''),
                pattern_type=pattern_data.get('pattern_type', ''),
                content_embedding=content_embedding,
                metadata_embedding=metadata_embedding,
                combined_embedding=combined_embedding,
                metadata=pattern_data.get('metadata', {}),
                performance_metrics=pattern_data.get('performance_metrics', {}),
                created_at=datetime.now(),
                last_accessed=datetime.now(),
                access_count=0
            )
        except Exception as e:
            self.logger.error(f"Pattern embedding failed: {e}")
            return None
    
    def _extract_content_text(self, pattern_data: Dict) -> str:
        """Extract text content from pattern data"""
        content_parts = []
        
        # Extract text from various fields
        if 'content_structure' in pattern_data:
            structure = pattern_data['content_structure']
            for key, value in structure.items():
                if isinstance(value, str):
                    content_parts.append(f"{key}: {value}")
                elif isinstance(value, list):
                    content_parts.append(f"{key}: {' '.join(value)}")
        
        if 'value_propositions' in pattern_data:
            content_parts.extend(pattern_data['value_propositions'])
        
        if 'description' in pattern_data:
            content_parts.append(pattern_data['description'])
        
        return ' '.join(content_parts)
    
    def _extract_metadata_text(self, pattern_data: Dict) -> str:
        """Extract metadata text from pattern data"""
        metadata_parts = []
        
        if 'pattern_type' in pattern_data:
            metadata_parts.append(f"type: {pattern_data['pattern_type']}")
        
        if 'industry' in pattern_data:
            metadata_parts.append(f"industry: {pattern_data['industry']}")
        
        if 'target_audience' in pattern_data:
            metadata_parts.append(f"audience: {pattern_data['target_audience']}")
        
        if 'performance_metrics' in pattern_data:
            metrics = pattern_data['performance_metrics']
            for key, value in metrics.items():
                metadata_parts.append(f"{key}: {value}")
        
        return ' '.join(metadata_parts)
    
    def _combine_embeddings(self, content_emb: List[float], metadata_emb: List[float]) -> List[float]:
        """Combine content and metadata embeddings"""
        if not content_emb or not metadata_emb:
            return content_emb or metadata_emb or []
        
        # Concatenate embeddings
        combined = content_emb + metadata_emb
        
        # Apply dimensionality reduction if too large
        if len(combined) > 1536:  # Reasonable limit
            combined_array = np.array(combined).reshape(1, -1)
            pca = PCA(n_components=1536)
            reduced = pca.fit_transform(combined_array)
            return reduced[0].tolist()
        
        return combined


class VectorDatabaseManager:
    """High-level manager for vector database operations"""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("vector_db_manager")
        
        # Initialize database
        db_type = config.get('db_type', VectorDBType.FAISS)
        if db_type == VectorDBType.PINECONE:
            self.db = PineconeVectorDB(config.get('pinecone', {}))
        elif db_type == VectorDBType.QDRANT:
            self.db = QdrantVectorDB(config.get('qdrant', {}))
        else:
            self.db = FAISSVectorDB(config.get('faiss', {}))
        
        # Initialize embedding service
        self.embedding_service = EmbeddingService(config.get('embeddings', {}))
        
        # Collections
        self.collections = {
            'learning_patterns': 'learning_patterns',
            'competitive_analysis': 'competitive_analysis',
            'trend_data': 'trend_data',
            'user_preferences': 'user_preferences'
        }
        
        # Performance tracking
        self.search_history = []
        self.performance_metrics = {
            'total_searches': 0,
            'avg_search_time': 0.0,
            'cache_hit_rate': 0.0
        }
        
        # Search cache
        self.search_cache = {}
        self.cache_ttl = 3600  # 1 hour
    
    async def initialize(self) -> bool:
        """Initialize vector database connection and collections"""
        try:
            # Connect to database
            if not await self.db.connect():
                return False
            
            # Create collections
            for collection_name in self.collections.values():
                await self.db.create_collection(collection_name, 1536)  # OpenAI embedding dimension
            
            self.logger.info("Vector database initialized successfully")
            return True
        except Exception as e:
            self.logger.error(f"Vector database initialization failed: {e}")
            return False
    
    async def store_learning_pattern(self, pattern_data: Dict[str, Any]) -> bool:
        """Store learning pattern with vector embedding"""
        try:
            # Generate pattern vector
            pattern_vector = await self.embedding_service.embed_pattern(pattern_data)
            if not pattern_vector:
                return False
            
            # Create vector record
            vector_record = VectorRecord(
                id=pattern_vector.pattern_id,
                vector=pattern_vector.combined_embedding,
                metadata={
                    'pattern_type': pattern_vector.pattern_type,
                    'metadata': pattern_vector.metadata,
                    'performance_metrics': pattern_vector.performance_metrics,
                    'created_at': pattern_vector.created_at.isoformat(),
                    'access_count': pattern_vector.access_count
                },
                timestamp=datetime.now(),
                collection=self.collections['learning_patterns']
            )
            
            # Store in vector database
            return await self.db.upsert_vectors([vector_record])
        except Exception as e:
            self.logger.error(f"Failed to store learning pattern: {e}")
            return False
    
    async def find_similar_patterns(
        self,
        query_text: str,
        pattern_type: Optional[str] = None,
        top_k: int = 5,
        min_score: float = 0.7
    ) -> List[Dict[str, Any]]:
        """Find similar learning patterns"""
        try:
            import time
            start_time = time.time()
            
            # Check cache
            cache_key = f"{hash(query_text)}_{pattern_type}_{top_k}"
            if cache_key in self.search_cache:
                cached_result = self.search_cache[cache_key]
                if time.time() - cached_result['timestamp'] < self.cache_ttl:
                    return cached_result['results']
            
            # Generate query embedding
            query_embedding = await self.embedding_service.embed_text(query_text)
            if not query_embedding:
                return []
            
            # Prepare filter
            filter_criteria = {}
            if pattern_type:
                filter_criteria['pattern_type'] = pattern_type
            
            # Search vector database
            search_results = await self.db.search(
                query_vector=query_embedding,
                collection=self.collections['learning_patterns'],
                top_k=top_k * 2,  # Get more to filter by score
                filter_criteria=filter_criteria
            )
            
            # Filter by minimum score and format results
            filtered_results = []
            for result in search_results:
                if result.score >= min_score:
                    filtered_results.append({
                        'pattern_id': result.id,
                        'similarity_score': result.score,
                        'pattern_type': result.metadata.get('pattern_type'),
                        'metadata': result.metadata.get('metadata', {}),
                        'performance_metrics': result.metadata.get('performance_metrics', {}),
                        'created_at': result.metadata.get('created_at'),
                        'access_count': result.metadata.get('access_count', 0)
                    })
            
            # Update cache
            self.search_cache[cache_key] = {
                'results': filtered_results[:top_k],
                'timestamp': time.time()
            }
            
            # Update performance metrics
            search_time = time.time() - start_time
            self._update_performance_metrics(search_time, len(filtered_results))
            
            return filtered_results[:top_k]
        except Exception as e:
            self.logger.error(f"Pattern search failed: {e}")
            return []
    
    async def store_competitive_analysis(self, analysis_data: Dict[str, Any]) -> bool:
        """Store competitive analysis with embedding"""
        try:
            # Extract text for embedding
            text_content = f"""
            Company: {analysis_data.get('company_name', '')}
            Industry: {analysis_data.get('industry', '')}
            Headline: {analysis_data.get('headline', '')}
            Value Props: {' '.join(analysis_data.get('value_propositions', []))}
            Key Features: {' '.join(analysis_data.get('unique_features', []))}
            """
            
            # Generate embedding
            embedding = await self.embedding_service.embed_text(text_content)
            if not embedding:
                return False
            
            # Create vector record
            vector_record = VectorRecord(
                id=analysis_data.get('analysis_id', f"comp_{hash(text_content)}"),
                vector=embedding,
                metadata=analysis_data,
                timestamp=datetime.now(),
                collection=self.collections['competitive_analysis']
            )
            
            return await self.db.upsert_vectors([vector_record])
        except Exception as e:
            self.logger.error(f"Failed to store competitive analysis: {e}")
            return False
    
    async def find_similar_competitors(
        self,
        query_analysis: Dict[str, Any],
        top_k: int = 5
    ) -> List[Dict[str, Any]]:
        """Find similar competitor analyses"""
        try:
            # Generate query embedding from analysis data
            query_text = f"""
            Industry: {query_analysis.get('industry', '')}
            Features: {' '.join(query_analysis.get('features', []))}
            Target: {query_analysis.get('target_audience', '')}
            """
            
            query_embedding = await self.embedding_service.embed_text(query_text)
            if not query_embedding:
                return []
            
            # Search for similar competitors
            results = await self.db.search(
                query_vector=query_embedding,
                collection=self.collections['competitive_analysis'],
                top_k=top_k
            )
            
            return [
                {
                    'analysis_id': result.id,
                    'similarity_score': result.score,
                    'competitor_data': result.metadata
                }
                for result in results
            ]
        except Exception as e:
            self.logger.error(f"Competitor search failed: {e}")
            return []
    
    async def get_database_stats(self) -> Dict[str, Any]:
        """Get comprehensive database statistics"""
        try:
            stats = {}
            
            for name, collection in self.collections.items():
                collection_stats = await self.db.get_collection_stats(collection)
                stats[name] = collection_stats
            
            # Add performance metrics
            stats['performance'] = self.performance_metrics
            stats['cache_size'] = len(self.search_cache)
            
            return stats
        except Exception as e:
            self.logger.error(f"Failed to get database stats: {e}")
            return {}
    
    def _update_performance_metrics(self, search_time: float, result_count: int):
        """Update performance tracking metrics"""
        self.performance_metrics['total_searches'] += 1
        
        # Update average search time
        current_avg = self.performance_metrics['avg_search_time']
        total_searches = self.performance_metrics['total_searches']
        
        new_avg = ((current_avg * (total_searches - 1)) + search_time) / total_searches
        self.performance_metrics['avg_search_time'] = new_avg
        
        # Track search results
        self.search_history.append({
            'timestamp': datetime.now(),
            'search_time': search_time,
            'result_count': result_count
        })
        
        # Keep only recent history
        if len(self.search_history) > 1000:
            self.search_history = self.search_history[-1000:]
    
    async def cleanup_old_vectors(self, days: int = 30) -> int:
        """Clean up old, unused vectors"""
        try:
            # This would implement cleanup logic
            # For now, just return 0
            return 0
        except Exception as e:
            self.logger.error(f"Cleanup failed: {e}")
            return 0


# Usage example
async def main():
    """Example usage of VectorDatabaseManager"""
    
    config = {
        'db_type': VectorDBType.FAISS,
        'embeddings': {
            'openai_api_key': 'your-openai-key',
            'sentence_model': 'all-MiniLM-L6-v2'
        }
    }
    
    # Initialize vector database
    vector_db = VectorDatabaseManager(config)
    await vector_db.initialize()
    
    # Store a learning pattern
    pattern_data = {
        'pattern_id': 'pattern_001',
        'pattern_type': 'direct_response',
        'content_structure': {
            'headline': 'Urgent benefit-focused headline',
            'cta': 'Limited time action'
        },
        'performance_metrics': {
            'conversion_rate': 0.15,
            'engagement_rate': 0.85
        },
        'metadata': {
            'industry': 'SaaS',
            'target_audience': 'small business'
        }
    }
    
    await vector_db.store_learning_pattern(pattern_data)
    
    # Search for similar patterns
    similar_patterns = await vector_db.find_similar_patterns(
        "urgent call to action for small business software",
        pattern_type="direct_response",
        top_k=3
    )
    
    print(f"Found {len(similar_patterns)} similar patterns")
    for pattern in similar_patterns:
        print(f"- {pattern['pattern_id']}: {pattern['similarity_score']:.3f}")
    
    # Get database stats
    stats = await vector_db.get_database_stats()
    print(f"Database stats: {stats}")


if __name__ == "__main__":
    asyncio.run(main())