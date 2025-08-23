"""
Competitive Analysis AI - Advanced Web Scraping and Strategy Generation
Real-time competitive intelligence with automatic differentiation strategies
"""

import asyncio
import logging
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime, timedelta
import json
import aiohttp
from dataclasses import dataclass, asdict
from enum import Enum
import re
from urllib.parse import urljoin, urlparse
import hashlib

# Web scraping
from playwright.async_api import async_playwright
from bs4 import BeautifulSoup
import requests

# NLP and analysis
import openai
from langchain.llms import OpenAI
from langchain.chains import LLMChain
from langchain.prompts import PromptTemplate
import nltk
from textstat import flesch_reading_ease, flesch_kincaid_grade

# Machine learning
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans
from sklearn.metrics.pairwise import cosine_similarity

# Image processing
from PIL import Image
import cv2


class AnalysisDepth(Enum):
    QUICK = "quick"      # Basic structure and headlines
    STANDARD = "standard"  # Full content and design analysis
    DEEP = "deep"        # Advanced behavioral and performance analysis


class CompetitorType(Enum):
    DIRECT = "direct"           # Same industry, same audience
    INDIRECT = "indirect"       # Same audience, different industry
    SUBSTITUTE = "substitute"   # Different solution, same problem
    ASPIRATIONAL = "aspirational"  # Industry leaders to learn from


@dataclass
class CompetitorProfile:
    domain: str
    company_name: str
    competitor_type: CompetitorType
    industry: str
    discovered_at: datetime
    last_analyzed: Optional[datetime]
    trust_score: float  # 0-1, based on domain authority and credibility
    threat_level: float  # 0-1, how much competition they pose
    innovation_score: float  # 0-1, how innovative their approach is


@dataclass
class LandingPageAnalysis:
    url: str
    title: str
    meta_description: str
    
    # Content analysis
    headline: str
    subheadlines: List[str]
    value_propositions: List[str]
    pain_points_addressed: List[str]
    social_proof_elements: List[str]
    cta_elements: List[Dict[str, Any]]
    content_length: int
    readability_score: float
    
    # Design analysis
    color_scheme: List[str]
    typography: Dict[str, Any]
    layout_type: str
    mobile_responsive: bool
    loading_speed: float
    
    # SEO analysis
    keywords: List[str]
    meta_tags: Dict[str, str]
    structured_data: Dict[str, Any]
    
    # Performance indicators
    estimated_traffic: Optional[int]
    conversion_elements_count: int
    trust_signals: List[str]
    
    # Innovation factors
    unique_features: List[str]
    ai_powered_elements: List[str]
    personalization_level: str
    
    analyzed_at: datetime
    confidence_score: float


@dataclass
class DifferentiationStrategy:
    strategy_id: str
    strategy_type: str  # 'positioning', 'features', 'messaging', 'design'
    title: str
    description: str
    implementation_steps: List[str]
    expected_impact: Dict[str, float]
    difficulty_level: str  # 'easy', 'medium', 'hard'
    time_to_implement: str  # '1d', '1w', '1m'
    competitive_advantage: float  # 0-1 score
    risk_level: str  # 'low', 'medium', 'high'


class CompetitiveAnalyzer:
    """
    Advanced competitive analysis AI with web scraping and strategy generation
    """
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.logger = logging.getLogger("competitive_analyzer")
        
        # Analysis cache to avoid re-scraping
        self.analysis_cache: Dict[str, LandingPageAnalysis] = {}
        self.competitor_profiles: Dict[str, CompetitorProfile] = {}
        
        # ML models for analysis
        self.tfidf_vectorizer = TfidfVectorizer(max_features=1000, stop_words='english')
        self.content_clusters = None
        
        # Web scraping setup
        self.playwright_browser = None
        self.session = None
        
        # OpenAI for advanced analysis
        self.openai_client = openai
        self.openai_client.api_key = config.get('openai_api_key')
        
        # Rate limiting
        self.last_request_time = {}
        self.min_request_interval = 2.0  # seconds between requests
        
    async def __aenter__(self):
        """Async context manager entry"""
        await self._initialize_browser()
        self.session = aiohttp.ClientSession()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit"""
        if self.playwright_browser:
            await self.playwright_browser.close()
        if self.session:
            await self.session.close()
    
    async def _initialize_browser(self):
        """Initialize Playwright browser for JavaScript-heavy sites"""
        try:
            playwright = await async_playwright().start()
            self.playwright_browser = await playwright.chromium.launch(
                headless=True,
                args=['--disable-dev-shm-usage', '--no-sandbox']
            )
            self.logger.info("Browser initialized successfully")
        except Exception as e:
            self.logger.error(f"Failed to initialize browser: {e}")
    
    async def discover_competitors(
        self, 
        industry: str, 
        keywords: List[str],
        target_audience: str,
        max_competitors: int = 10
    ) -> List[CompetitorProfile]:
        """
        Discover competitors using multiple methods:
        1. Google search with industry keywords
        2. Social media analysis
        3. Patent and trademark searches
        4. Industry directory scraping
        """
        try:
            self.logger.info(f"Discovering competitors for {industry} industry")
            
            discovered_competitors = []
            
            # Method 1: Google search discovery
            search_competitors = await self._discover_via_google_search(
                industry, keywords, target_audience
            )
            discovered_competitors.extend(search_competitors)
            
            # Method 2: Industry directory scraping
            directory_competitors = await self._discover_via_directories(industry)
            discovered_competitors.extend(directory_competitors)
            
            # Method 3: Social media mining
            social_competitors = await self._discover_via_social_media(
                industry, keywords
            )
            discovered_competitors.extend(social_competitors)
            
            # Remove duplicates and rank by relevance
            unique_competitors = self._deduplicate_competitors(discovered_competitors)
            ranked_competitors = await self._rank_competitors_by_threat(
                unique_competitors, industry, target_audience
            )
            
            # Store competitor profiles
            for competitor in ranked_competitors[:max_competitors]:
                self.competitor_profiles[competitor.domain] = competitor
            
            self.logger.info(f"Discovered {len(ranked_competitors)} competitors")
            return ranked_competitors[:max_competitors]
            
        except Exception as e:
            self.logger.error(f"Competitor discovery failed: {e}")
            return []
    
    async def _discover_via_google_search(
        self, 
        industry: str, 
        keywords: List[str],
        target_audience: str
    ) -> List[CompetitorProfile]:
        """Discover competitors via Google search API or scraping"""
        competitors = []
        
        try:
            search_queries = [
                f"{industry} {keyword} landing page" for keyword in keywords[:3]
            ] + [
                f"best {industry} companies",
                f"{industry} solutions for {target_audience}",
                f"top {industry} providers"
            ]
            
            for query in search_queries:
                # Simulate search results (in production, use Google Custom Search API)
                search_results = await self._simulate_google_search(query)
                
                for result in search_results:
                    domain = urlparse(result['url']).netloc
                    
                    competitor = CompetitorProfile(
                        domain=domain,
                        company_name=result.get('title', domain),
                        competitor_type=CompetitorType.DIRECT,
                        industry=industry,
                        discovered_at=datetime.now(),
                        last_analyzed=None,
                        trust_score=0.7,  # Will be calculated later
                        threat_level=0.5,  # Will be calculated later
                        innovation_score=0.5  # Will be calculated later
                    )
                    
                    competitors.append(competitor)
                
                # Rate limiting
                await asyncio.sleep(1)
            
            return competitors
            
        except Exception as e:
            self.logger.error(f"Google search discovery failed: {e}")
            return []
    
    async def _simulate_google_search(self, query: str) -> List[Dict[str, str]]:
        """Simulate Google search results (replace with actual API in production)"""
        # This is a placeholder - in production, integrate with Google Custom Search API
        return [
            {"url": "https://example-competitor1.com", "title": "Competitor 1 - Industry Leader"},
            {"url": "https://example-competitor2.com", "title": "Competitor 2 - Innovative Solution"},
            {"url": "https://example-competitor3.com", "title": "Competitor 3 - Market Challenger"}
        ]
    
    async def analyze_competitor_lp(
        self, 
        url: str, 
        depth: AnalysisDepth = AnalysisDepth.STANDARD
    ) -> LandingPageAnalysis:
        """
        Comprehensive landing page analysis with multiple techniques
        """
        try:
            self.logger.info(f"Analyzing competitor LP: {url} (depth: {depth.value})")
            
            # Check cache first
            cache_key = f"{url}_{depth.value}"
            if cache_key in self.analysis_cache:
                cached_analysis = self.analysis_cache[cache_key]
                if (datetime.now() - cached_analysis.analyzed_at).hours < 24:
                    self.logger.info("Using cached analysis")
                    return cached_analysis
            
            # Rate limiting
            await self._apply_rate_limit(url)
            
            # Fetch page content
            page_content = await self._fetch_page_content(url)
            if not page_content:
                raise Exception("Failed to fetch page content")
            
            # Parse with BeautifulSoup
            soup = BeautifulSoup(page_content['html'], 'html.parser')
            
            # Basic content extraction
            basic_analysis = await self._extract_basic_content(soup, url)
            
            # Design analysis
            design_analysis = await self._analyze_design_elements(soup, page_content)
            
            # SEO analysis
            seo_analysis = await self._analyze_seo_elements(soup)
            
            # Advanced analysis based on depth
            advanced_analysis = {}
            if depth in [AnalysisDepth.STANDARD, AnalysisDepth.DEEP]:
                advanced_analysis = await self._perform_advanced_analysis(
                    soup, page_content, url
                )
            
            # AI-powered insights
            ai_insights = await self._generate_ai_insights(
                basic_analysis, design_analysis, seo_analysis
            )
            
            # Combine all analyses
            analysis = LandingPageAnalysis(
                url=url,
                **basic_analysis,
                **design_analysis,
                **seo_analysis,
                **advanced_analysis,
                **ai_insights,
                analyzed_at=datetime.now(),
                confidence_score=self._calculate_analysis_confidence(basic_analysis)
            )
            
            # Cache the result
            self.analysis_cache[cache_key] = analysis
            
            self.logger.info(f"Analysis completed for {url}")
            return analysis
            
        except Exception as e:
            self.logger.error(f"LP analysis failed for {url}: {e}")
            raise
    
    async def _fetch_page_content(self, url: str) -> Optional[Dict[str, Any]]:
        """Fetch page content using both requests and Playwright"""
        try:
            # First try with requests (faster)
            async with self.session.get(url, timeout=10) as response:
                if response.status == 200:
                    html = await response.text()
                    
                    # Check if JavaScript rendering is needed
                    if self._needs_javascript_rendering(html):
                        # Use Playwright for JavaScript-heavy pages
                        return await self._fetch_with_playwright(url)
                    else:
                        return {
                            'html': html,
                            'loading_speed': response.headers.get('X-Response-Time', 0),
                            'status_code': response.status
                        }
            
            return None
            
        except Exception as e:
            self.logger.error(f"Failed to fetch {url}: {e}")
            return None
    
    async def _fetch_with_playwright(self, url: str) -> Dict[str, Any]:
        """Fetch content with JavaScript rendering"""
        if not self.playwright_browser:
            await self._initialize_browser()
        
        page = await self.playwright_browser.new_page()
        
        try:
            start_time = datetime.now()
            await page.goto(url, wait_until='networkidle')
            load_time = (datetime.now() - start_time).total_seconds()
            
            html = await page.content()
            
            return {
                'html': html,
                'loading_speed': load_time,
                'status_code': 200
            }
            
        finally:
            await page.close()
    
    def _needs_javascript_rendering(self, html: str) -> bool:
        """Detect if page needs JavaScript rendering"""
        # Simple heuristics - in production, use more sophisticated detection
        js_indicators = [
            'React', 'Vue', 'Angular', 'spa-loader',
            'window.render', 'document.createElement',
            'loading...', 'Please enable JavaScript'
        ]
        
        return any(indicator in html for indicator in js_indicators)
    
    async def _extract_basic_content(self, soup: BeautifulSoup, url: str) -> Dict[str, Any]:
        """Extract basic content elements"""
        
        # Title and meta
        title = soup.find('title')
        title_text = title.get_text().strip() if title else ""
        
        meta_desc = soup.find('meta', attrs={'name': 'description'})
        meta_description = meta_desc.get('content', '') if meta_desc else ""
        
        # Headlines
        h1_tags = soup.find_all('h1')
        headline = h1_tags[0].get_text().strip() if h1_tags else ""
        
        h2_tags = soup.find_all('h2')
        subheadlines = [h2.get_text().strip() for h2 in h2_tags[:5]]
        
        # CTAs
        cta_elements = []
        buttons = soup.find_all(['button', 'a'], class_=re.compile(r'btn|button|cta', re.I))
        for btn in buttons[:10]:
            cta_elements.append({
                'text': btn.get_text().strip(),
                'type': btn.name,
                'classes': btn.get('class', []),
                'href': btn.get('href', ''),
                'position': 'header' if btn.find_parent('header') else 'body'
            })
        
        # Content analysis
        content_text = soup.get_text()
        content_length = len(content_text.split())
        readability_score = flesch_reading_ease(content_text) if content_text else 0
        
        # Value propositions (using NLP)
        value_props = self._extract_value_propositions(content_text)
        pain_points = self._extract_pain_points(content_text)
        
        # Social proof
        social_proof = self._extract_social_proof_elements(soup)
        
        return {
            'title': title_text,
            'meta_description': meta_description,
            'headline': headline,
            'subheadlines': subheadlines,
            'value_propositions': value_props,
            'pain_points_addressed': pain_points,
            'social_proof_elements': social_proof,
            'cta_elements': cta_elements,
            'content_length': content_length,
            'readability_score': readability_score
        }
    
    def _extract_value_propositions(self, text: str) -> List[str]:
        """Extract value propositions using NLP patterns"""
        # Simple pattern matching - in production, use more sophisticated NLP
        value_patterns = [
            r"save (\d+%|\$[\d,]+|time|money)",
            r"increase (sales|revenue|conversion|efficiency) by (\d+%)",
            r"get (instant|immediate|fast|quick) (results|access|delivery)",
            r"(free|no cost|zero fee|complimentary) (trial|consultation|audit)",
            r"(guaranteed|promise|ensure) (results|success|satisfaction)"
        ]
        
        value_props = []
        text_lower = text.lower()
        
        for pattern in value_patterns:
            matches = re.findall(pattern, text_lower)
            for match in matches:
                if isinstance(match, tuple):
                    value_props.append(" ".join(match))
                else:
                    value_props.append(match)
        
        return value_props[:5]  # Limit to top 5
    
    def _extract_pain_points(self, text: str) -> List[str]:
        """Extract addressed pain points"""
        pain_patterns = [
            r"tired of ([\w\s]{5,50})\?",
            r"struggling with ([\w\s]{5,50})\?",
            r"problem with ([\w\s]{5,50})",
            r"frustrated by ([\w\s]{5,50})",
            r"difficulty ([\w\s]{5,50})"
        ]
        
        pain_points = []
        text_lower = text.lower()
        
        for pattern in pain_patterns:
            matches = re.findall(pattern, text_lower)
            pain_points.extend(matches)
        
        return pain_points[:5]
    
    def _extract_social_proof_elements(self, soup: BeautifulSoup) -> List[str]:
        """Extract social proof elements"""
        social_proof = []
        
        # Testimonials
        testimonial_selectors = [
            '[class*="testimonial"]',
            '[class*="review"]',
            '[class*="quote"]',
            'blockquote'
        ]
        
        for selector in testimonial_selectors:
            elements = soup.select(selector)
            for elem in elements[:3]:
                text = elem.get_text().strip()
                if 20 < len(text) < 200:  # Reasonable testimonial length
                    social_proof.append(f"Testimonial: {text[:100]}...")
        
        # Numbers/stats
        numbers = re.findall(r'(\d{1,3}(?:,\d{3})*(?:\+|k|K|M|million|billion)?)', soup.get_text())
        if numbers:
            social_proof.append(f"Statistics: {', '.join(numbers[:5])}")
        
        # Client logos (count img tags with client/logo classes)
        logo_imgs = soup.find_all('img', class_=re.compile(r'client|logo|partner', re.I))
        if logo_imgs:
            social_proof.append(f"Client logos: {len(logo_imgs)} displayed")
        
        return social_proof
    
    async def _analyze_design_elements(self, soup: BeautifulSoup, page_content: Dict) -> Dict[str, Any]:
        """Analyze design and visual elements"""
        
        # Color scheme extraction (basic implementation)
        style_tags = soup.find_all('style')
        css_content = ' '.join([style.get_text() for style in style_tags])
        colors = re.findall(r'#[0-9a-fA-F]{6}|#[0-9a-fA-F]{3}', css_content)
        
        # Typography analysis
        font_families = re.findall(r'font-family:\s*([^;]+)', css_content)
        
        # Layout detection
        layout_type = "single_column"  # Default
        if soup.find(class_=re.compile(r'sidebar|col-md-|grid', re.I)):
            layout_type = "multi_column"
        if soup.find(class_=re.compile(r'hero|jumbotron', re.I)):
            layout_type = "hero_with_content"
        
        # Mobile responsiveness check
        viewport_meta = soup.find('meta', attrs={'name': 'viewport'})
        has_responsive_meta = viewport_meta is not None
        has_responsive_css = 'media' in css_content and '@media' in css_content
        mobile_responsive = has_responsive_meta and has_responsive_css
        
        return {
            'color_scheme': list(set(colors))[:10],
            'typography': {
                'font_families': list(set(font_families))[:5],
                'heading_count': len(soup.find_all(['h1', 'h2', 'h3', 'h4', 'h5', 'h6']))
            },
            'layout_type': layout_type,
            'mobile_responsive': mobile_responsive,
            'loading_speed': page_content.get('loading_speed', 0)
        }
    
    async def _analyze_seo_elements(self, soup: BeautifulSoup) -> Dict[str, Any]:
        """Analyze SEO elements"""
        
        # Meta tags
        meta_tags = {}
        for meta in soup.find_all('meta'):
            name = meta.get('name') or meta.get('property')
            content = meta.get('content')
            if name and content:
                meta_tags[name] = content
        
        # Keywords extraction (basic)
        text_content = soup.get_text().lower()
        words = re.findall(r'\b[a-z]{3,}\b', text_content)
        word_freq = {}
        for word in words:
            word_freq[word] = word_freq.get(word, 0) + 1
        
        # Get top keywords (excluding common stop words)
        stop_words = {'the', 'and', 'you', 'your', 'our', 'for', 'with', 'are', 'can', 'will', 'get', 'more', 'new', 'best'}
        keywords = [word for word, freq in sorted(word_freq.items(), key=lambda x: x[1], reverse=True) 
                   if word not in stop_words][:10]
        
        # Structured data
        structured_data = {}
        json_ld_scripts = soup.find_all('script', type='application/ld+json')
        for script in json_ld_scripts:
            try:
                data = json.loads(script.string)
                structured_data.update(data)
            except:
                pass
        
        return {
            'keywords': keywords,
            'meta_tags': meta_tags,
            'structured_data': structured_data
        }
    
    async def _perform_advanced_analysis(
        self, 
        soup: BeautifulSoup, 
        page_content: Dict,
        url: str
    ) -> Dict[str, Any]:
        """Perform advanced analysis for deeper insights"""
        
        # Estimate traffic (placeholder - integrate with real tools)
        estimated_traffic = None  # Would integrate with SEMrush/Ahrefs API
        
        # Trust signals detection
        trust_signals = []
        trust_indicators = [
            'ssl', 'secure', 'guarantee', 'certified', 'accredited',
            'testimonial', 'review', 'award', 'since', 'founded'
        ]
        
        text_lower = soup.get_text().lower()
        for indicator in trust_indicators:
            if indicator in text_lower:
                trust_signals.append(indicator)
        
        # Unique features detection
        unique_features = await self._detect_unique_features(soup)
        
        # AI elements detection
        ai_elements = self._detect_ai_powered_elements(soup)
        
        # Personalization level
        personalization_level = self._assess_personalization_level(soup)
        
        return {
            'estimated_traffic': estimated_traffic,
            'conversion_elements_count': len(soup.find_all(['button', 'form', 'input[type="submit"]'])),
            'trust_signals': trust_signals,
            'unique_features': unique_features,
            'ai_powered_elements': ai_elements,
            'personalization_level': personalization_level
        }
    
    async def _detect_unique_features(self, soup: BeautifulSoup) -> List[str]:
        """Detect unique or innovative features"""
        unique_features = []
        
        # Interactive elements
        if soup.find(['canvas', 'svg']):
            unique_features.append('Interactive graphics/animations')
        
        # Chat/messaging
        if soup.find(class_=re.compile(r'chat|message|intercom', re.I)):
            unique_features.append('Live chat integration')
        
        # Video elements
        if soup.find(['video', 'iframe[src*="youtube"]', 'iframe[src*="vimeo"]']):
            unique_features.append('Video content')
        
        # Calculator/tools
        if soup.find(['input[type="range"]', 'input[type="number"]']) and soup.find('button'):
            unique_features.append('Interactive calculator/tool')
        
        # Progressive web app indicators
        if soup.find('link[rel="manifest"]'):
            unique_features.append('Progressive Web App')
        
        return unique_features
    
    def _detect_ai_powered_elements(self, soup: BeautifulSoup) -> List[str]:
        """Detect AI-powered elements"""
        ai_elements = []
        text_content = soup.get_text().lower()
        
        ai_indicators = [
            ('chatbot', 'AI Chatbot'),
            ('recommendation', 'AI Recommendations'),
            ('personalized', 'AI Personalization'),
            ('machine learning', 'Machine Learning'),
            ('artificial intelligence', 'AI Features'),
            ('smart', 'Smart Features'),
            ('predictive', 'Predictive Analytics')
        ]
        
        for indicator, feature in ai_indicators:
            if indicator in text_content:
                ai_elements.append(feature)
        
        return ai_elements
    
    def _assess_personalization_level(self, soup: BeautifulSoup) -> str:
        """Assess level of personalization"""
        
        # Check for personalization indicators
        personalization_score = 0
        
        # Dynamic content areas
        if soup.find(class_=re.compile(r'dynamic|personal|user-specific', re.I)):
            personalization_score += 2
        
        # Forms for data collection
        forms = soup.find_all('form')
        if forms:
            personalization_score += len(forms)
        
        # Cookies/privacy notices (indicates tracking)
        if soup.find(class_=re.compile(r'cookie|privacy|gdpr', re.I)):
            personalization_score += 1
        
        # JavaScript that might handle personalization
        scripts = soup.find_all('script')
        for script in scripts:
            if script.string and any(keyword in script.string.lower() 
                                   for keyword in ['localStorage', 'sessionStorage', 'user']):
                personalization_score += 1
                break
        
        if personalization_score >= 4:
            return "high"
        elif personalization_score >= 2:
            return "medium"
        else:
            return "low"
    
    async def _generate_ai_insights(self, *analyses) -> Dict[str, Any]:
        """Generate AI-powered insights from analysis data"""
        try:
            # Combine all analysis data
            combined_data = {}
            for analysis in analyses:
                combined_data.update(analysis)
            
            # Generate insights using GPT-4
            insights_prompt = f"""
            Analyze this landing page data and provide key insights:
            
            {json.dumps(combined_data, ensure_ascii=False, indent=2)}
            
            Provide insights on:
            1. Content effectiveness score (0-10)
            2. Design modernness score (0-10)
            3. Conversion optimization level (0-10)
            4. Innovation factors
            5. Competitive strengths
            6. Potential weaknesses
            
            Format as JSON.
            """
            
            response = await self.openai_client.chat.completions.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are a landing page analysis expert."},
                    {"role": "user", "content": insights_prompt}
                ],
                temperature=0.3,
                max_tokens=1000
            )
            
            insights_text = response.choices[0].message.content
            insights = json.loads(insights_text)
            
            return insights
            
        except Exception as e:
            self.logger.error(f"Failed to generate AI insights: {e}")
            return {
                'content_effectiveness_score': 5.0,
                'design_modernness_score': 5.0,
                'conversion_optimization_level': 5.0,
                'innovation_factors': [],
                'competitive_strengths': [],
                'potential_weaknesses': []
            }
    
    def _calculate_analysis_confidence(self, basic_analysis: Dict) -> float:
        """Calculate confidence score for the analysis"""
        confidence_factors = []
        
        # Content completeness
        if basic_analysis.get('headline'):
            confidence_factors.append(1.0)
        if basic_analysis.get('title'):
            confidence_factors.append(1.0)
        if basic_analysis.get('cta_elements'):
            confidence_factors.append(1.0)
        if basic_analysis.get('content_length', 0) > 100:
            confidence_factors.append(1.0)
        
        return sum(confidence_factors) / 4.0 if confidence_factors else 0.0
    
    async def generate_differentiation_strategies(
        self,
        our_analysis: Dict[str, Any],
        competitor_analyses: List[LandingPageAnalysis],
        industry: str,
        target_audience: str
    ) -> List[DifferentiationStrategy]:
        """
        Generate strategic differentiation recommendations
        """
        try:
            self.logger.info("Generating differentiation strategies")
            
            # Analyze competitive landscape
            competitive_gaps = await self._identify_competitive_gaps(
                competitor_analyses
            )
            
            # Identify our strengths and weaknesses
            our_position = await self._analyze_our_position(
                our_analysis, competitor_analyses
            )
            
            # Generate strategies using AI
            strategies = await self._generate_strategic_recommendations(
                competitive_gaps, our_position, industry, target_audience
            )
            
            # Rank strategies by impact and feasibility
            ranked_strategies = self._rank_strategies_by_impact(strategies)
            
            self.logger.info(f"Generated {len(ranked_strategies)} differentiation strategies")
            return ranked_strategies
            
        except Exception as e:
            self.logger.error(f"Strategy generation failed: {e}")
            return []
    
    async def _identify_competitive_gaps(
        self, 
        competitor_analyses: List[LandingPageAnalysis]
    ) -> Dict[str, Any]:
        """Identify gaps in competitive landscape"""
        
        # Collect all features and approaches
        all_features = []
        all_value_props = []
        all_cta_approaches = []
        design_trends = []
        
        for analysis in competitor_analyses:
            all_features.extend(analysis.unique_features)
            all_value_props.extend(analysis.value_propositions)
            all_cta_approaches.extend([cta['text'] for cta in analysis.cta_elements])
            design_trends.append(analysis.layout_type)
        
        # Find underrepresented approaches
        feature_freq = {}
        for feature in all_features:
            feature_freq[feature] = feature_freq.get(feature, 0) + 1
        
        # Gaps are features used by few competitors
        underutilized_features = [
            feature for feature, freq in feature_freq.items() 
            if freq <= len(competitor_analyses) * 0.3
        ]
        
        return {
            'underutilized_features': underutilized_features,
            'common_value_props': list(set(all_value_props)),
            'design_trends': list(set(design_trends)),
            'cta_saturation': len(set(all_cta_approaches)) / len(all_cta_approaches) if all_cta_approaches else 0
        }
    
    async def _analyze_our_position(
        self,
        our_analysis: Dict[str, Any],
        competitor_analyses: List[LandingPageAnalysis]
    ) -> Dict[str, Any]:
        """Analyze our current competitive position"""
        
        # Calculate our relative scores
        our_scores = {
            'content_length': our_analysis.get('content_length', 0),
            'cta_count': len(our_analysis.get('cta_elements', [])),
            'trust_signals': len(our_analysis.get('trust_signals', [])),
            'unique_features': len(our_analysis.get('unique_features', []))
        }
        
        # Compare with competitors
        competitor_averages = {}
        for metric in our_scores.keys():
            values = []
            for comp in competitor_analyses:
                if hasattr(comp, metric):
                    values.append(getattr(comp, metric, 0))
                elif metric in comp.__dict__:
                    values.append(len(comp.__dict__[metric]) if isinstance(comp.__dict__[metric], list) else comp.__dict__[metric])
            
            competitor_averages[metric] = np.mean(values) if values else 0
        
        # Identify our strengths and weaknesses
        strengths = []
        weaknesses = []
        
        for metric, our_score in our_scores.items():
            comp_avg = competitor_averages.get(metric, 0)
            if our_score > comp_avg * 1.2:
                strengths.append(metric)
            elif our_score < comp_avg * 0.8:
                weaknesses.append(metric)
        
        return {
            'our_scores': our_scores,
            'competitor_averages': competitor_averages,
            'strengths': strengths,
            'weaknesses': weaknesses
        }
    
    async def _generate_strategic_recommendations(
        self,
        competitive_gaps: Dict[str, Any],
        our_position: Dict[str, Any],
        industry: str,
        target_audience: str
    ) -> List[DifferentiationStrategy]:
        """Generate strategic recommendations using AI"""
        
        try:
            strategy_prompt = f"""
            Generate 5 differentiation strategies based on this competitive analysis:
            
            Industry: {industry}
            Target Audience: {target_audience}
            
            Competitive Gaps:
            {json.dumps(competitive_gaps, indent=2)}
            
            Our Position:
            {json.dumps(our_position, indent=2)}
            
            For each strategy, provide:
            1. strategy_type (positioning/features/messaging/design)
            2. title (brief, compelling)
            3. description (detailed explanation)
            4. implementation_steps (3-5 specific steps)
            5. expected_impact (conversion_lift, differentiation_score, market_position)
            6. difficulty_level (easy/medium/hard)
            7. time_to_implement (1d/1w/1m/3m)
            8. competitive_advantage (0-1 score)
            9. risk_level (low/medium/high)
            
            Format as JSON array.
            """
            
            response = await self.openai_client.chat.completions.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are a competitive strategy expert specializing in landing page optimization."},
                    {"role": "user", "content": strategy_prompt}
                ],
                temperature=0.7,
                max_tokens=2000
            )
            
            strategies_json = response.choices[0].message.content
            strategies_data = json.loads(strategies_json)
            
            strategies = []
            for i, strategy_data in enumerate(strategies_data):
                strategy = DifferentiationStrategy(
                    strategy_id=f"strategy_{i+1}",
                    strategy_type=strategy_data.get('strategy_type', 'features'),
                    title=strategy_data.get('title', ''),
                    description=strategy_data.get('description', ''),
                    implementation_steps=strategy_data.get('implementation_steps', []),
                    expected_impact=strategy_data.get('expected_impact', {}),
                    difficulty_level=strategy_data.get('difficulty_level', 'medium'),
                    time_to_implement=strategy_data.get('time_to_implement', '1w'),
                    competitive_advantage=strategy_data.get('competitive_advantage', 0.5),
                    risk_level=strategy_data.get('risk_level', 'medium')
                )
                strategies.append(strategy)
            
            return strategies
            
        except Exception as e:
            self.logger.error(f"Failed to generate strategic recommendations: {e}")
            return []
    
    def _rank_strategies_by_impact(
        self, 
        strategies: List[DifferentiationStrategy]
    ) -> List[DifferentiationStrategy]:
        """Rank strategies by potential impact and feasibility"""
        
        def calculate_strategy_score(strategy: DifferentiationStrategy) -> float:
            # Impact score
            impact_score = strategy.competitive_advantage * 0.4
            
            # Feasibility score (easier = higher score)
            difficulty_scores = {'easy': 1.0, 'medium': 0.7, 'hard': 0.4}
            feasibility_score = difficulty_scores.get(strategy.difficulty_level, 0.5)
            
            # Time factor (faster = higher score)
            time_scores = {'1d': 1.0, '1w': 0.8, '1m': 0.6, '3m': 0.4}
            time_score = time_scores.get(strategy.time_to_implement, 0.5)
            
            # Risk factor (lower risk = higher score)
            risk_scores = {'low': 1.0, 'medium': 0.7, 'high': 0.4}
            risk_score = risk_scores.get(strategy.risk_level, 0.5)
            
            # Combined score
            total_score = (
                impact_score * 0.4 +
                feasibility_score * 0.3 +
                time_score * 0.2 +
                risk_score * 0.1
            )
            
            return total_score
        
        # Sort by calculated score
        scored_strategies = [
            (strategy, calculate_strategy_score(strategy)) 
            for strategy in strategies
        ]
        
        scored_strategies.sort(key=lambda x: x[1], reverse=True)
        
        return [strategy for strategy, score in scored_strategies]
    
    async def _apply_rate_limit(self, url: str):
        """Apply rate limiting to avoid overwhelming servers"""
        domain = urlparse(url).netloc
        
        if domain in self.last_request_time:
            time_since_last = datetime.now().timestamp() - self.last_request_time[domain]
            if time_since_last < self.min_request_interval:
                wait_time = self.min_request_interval - time_since_last
                await asyncio.sleep(wait_time)
        
        self.last_request_time[domain] = datetime.now().timestamp()
    
    def _deduplicate_competitors(
        self, 
        competitors: List[CompetitorProfile]
    ) -> List[CompetitorProfile]:
        """Remove duplicate competitors"""
        seen_domains = set()
        unique_competitors = []
        
        for competitor in competitors:
            if competitor.domain not in seen_domains:
                seen_domains.add(competitor.domain)
                unique_competitors.append(competitor)
        
        return unique_competitors
    
    async def _rank_competitors_by_threat(
        self,
        competitors: List[CompetitorProfile],
        industry: str,
        target_audience: str
    ) -> List[CompetitorProfile]:
        """Rank competitors by threat level"""
        
        # For now, use simple ranking
        # In production, integrate with SEO tools, social metrics, etc.
        
        for competitor in competitors:
            # Calculate threat score based on various factors
            threat_score = 0.5  # Base score
            
            # Domain authority (would integrate with Moz/Ahrefs)
            # For now, use domain age heuristic
            if any(word in competitor.company_name.lower() for word in ['inc', 'corp', 'ltd', 'llc']):
                threat_score += 0.2
            
            # Industry relevance
            if industry.lower() in competitor.company_name.lower():
                threat_score += 0.3
            
            competitor.threat_level = min(threat_score, 1.0)
        
        # Sort by threat level
        competitors.sort(key=lambda x: x.threat_level, reverse=True)
        
        return competitors
    
    async def _discover_via_directories(self, industry: str) -> List[CompetitorProfile]:
        """Discover competitors via industry directories"""
        # Placeholder - would integrate with industry-specific directories
        return []
    
    async def _discover_via_social_media(
        self, 
        industry: str, 
        keywords: List[str]
    ) -> List[CompetitorProfile]:
        """Discover competitors via social media monitoring"""
        # Placeholder - would integrate with social media APIs
        return []


# Usage example
async def main():
    """Example usage of CompetitiveAnalyzer"""
    
    config = {
        'openai_api_key': 'your-openai-key',
        'google_api_key': 'your-google-key',
        'semrush_api_key': 'your-semrush-key'
    }
    
    async with CompetitiveAnalyzer(config) as analyzer:
        # Discover competitors
        competitors = await analyzer.discover_competitors(
            industry="SaaS",
            keywords=["project management", "team collaboration"],
            target_audience="small businesses",
            max_competitors=5
        )
        
        # Analyze competitor landing pages
        analyses = []
        for competitor in competitors:
            try:
                analysis = await analyzer.analyze_competitor_lp(
                    f"https://{competitor.domain}",
                    depth=AnalysisDepth.STANDARD
                )
                analyses.append(analysis)
            except Exception as e:
                print(f"Failed to analyze {competitor.domain}: {e}")
        
        # Generate differentiation strategies
        our_analysis = {
            'content_length': 800,
            'cta_elements': [{'text': 'Start Free Trial'}],
            'trust_signals': ['testimonials', 'security'],
            'unique_features': ['real-time collaboration']
        }
        
        strategies = await analyzer.generate_differentiation_strategies(
            our_analysis=our_analysis,
            competitor_analyses=analyses,
            industry="SaaS",
            target_audience="small businesses"
        )
        
        # Print results
        print(f"Discovered {len(competitors)} competitors")
        print(f"Analyzed {len(analyses)} landing pages")
        print(f"Generated {len(strategies)} differentiation strategies")
        
        for strategy in strategies:
            print(f"\nStrategy: {strategy.title}")
            print(f"Type: {strategy.strategy_type}")
            print(f"Impact: {strategy.competitive_advantage:.2f}")
            print(f"Difficulty: {strategy.difficulty_level}")


if __name__ == "__main__":
    asyncio.run(main())