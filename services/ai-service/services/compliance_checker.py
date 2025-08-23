from typing import Dict, Any, List

class ComplianceChecker:
    def __init__(self):
        self.rules = {
            "pharmaceutical": ["効果", "効能", "治療", "診断"],
            "misleading": ["絶対", "必ず", "100%", "保証"],
            "legal": ["特定商取引法", "個人情報保護"]
        }
    
    async def check(self, content: str) -> Dict[str, Any]:
        """Check content for compliance issues"""
        issues = []
        
        for category, keywords in self.rules.items():
            for keyword in keywords:
                if keyword in content:
                    issues.append({
                        "category": category,
                        "keyword": keyword,
                        "severity": "warning"
                    })
        
        return {
            "compliant": len(issues) == 0,
            "issues": issues,
            "score": 100 - (len(issues) * 10)
        }