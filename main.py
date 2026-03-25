import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core.config import get_settings
from app.core.logging import setup_logging
from app.api.routes import api_router

settings = get_settings()
setup_logging(debug=settings.debug)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown events."""
    logger.info(f"Starting {settings.app_name} v{settings.app_version}")
    logger.info(f"Debug mode: {settings.debug}")
    yield
    logger.info("Shutting down...")


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="""
## MyBizz Rental & Bills Chatbot API

An intelligent chatbot API powered by **Groq LLM** + **RAG (Retrieval Augmented Generation)**
that answers natural language questions about your Google Sheets business data.

### 🚀 Quick Start

1. **Load your data** → `POST /api/v1/documents/load`
2. **Ask a question** → `POST /api/v1/chat/query`

### 📊 Data Sources

Connects to your Google Spreadsheet with these sheets:
- **Sheet1** — Bill records (amounts, dates, status, categories)
- **Sheet2** — User/tenant information
- **Sheet3** — Property/rental details
- **Sheet4** — Additional records
- **BillHistory** — Historical billing data
- **Sheet5** — Miscellaneous data

### 💡 Example Queries

- *"Give me all users living on rent"*
- *"What is the total amount paid in March 2026?"*
- *"Show all unpaid bills"*
- *"List Worker Payment bills above ₹5000"*
- *"Which user paid the most bills?"*
""",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
    lifespan=lifespan,
)

# ── CORS Middleware ────────────────────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Update in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Routes ────────────────────────────────────────────────────────────────────
app.include_router(api_router)


@app.get("/", tags=["Root"], summary="API root")
def root():
    return {
        "app": settings.app_name,
        "version": settings.app_version,
        "docs": "/docs",
        "redoc": "/redoc",
        "health": "/api/v1/health/",
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug,
    )